package com.xyz.gumall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.xyz.common.exception.NoStockException;
import com.xyz.common.to.mq.OrderTo;
import com.xyz.common.to.mq.SeckillOrderTo;
import com.xyz.common.utils.R;
import com.xyz.common.vo.MemberRespVo;
import com.xyz.gumall.order.constant.OrderConstant;
import com.xyz.gumall.order.entity.OrderItemEntity;
import com.xyz.gumall.order.entity.PaymentInfoEntity;
import com.xyz.gumall.order.enume.OrderStatusEnum;
import com.xyz.gumall.order.feign.CartFeignService;
import com.xyz.gumall.order.feign.MemberFeignService;
import com.xyz.gumall.order.feign.ProductFeignService;
import com.xyz.gumall.order.feign.WmsFeignService;
import com.xyz.gumall.order.interceptor.LoginUserInterceptor;
import com.xyz.gumall.order.service.OrderItemService;
import com.xyz.gumall.order.service.PaymentInfoService;
import com.xyz.gumall.order.to.OrderCreateTo;
import com.xyz.gumall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xyz.common.utils.PageUtils;
import com.xyz.common.utils.Query;

import com.xyz.gumall.order.dao.OrderDao;
import com.xyz.gumall.order.entity.OrderEntity;
import com.xyz.gumall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

//@DS("one")
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    MemberFeignService memberFeignService;
    @Autowired
    CartFeignService cartFeignService;
    @Autowired
    ThreadPoolExecutor executor;
    @Autowired
    WmsFeignService wmsFeignService;
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    ProductFeignService productFeignService;
    @Autowired
    OrderItemService orderItemService;
    @Autowired
    PaymentInfoService paymentInfoService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();

        CompletableFuture<Void> getAddressFuture = CompletableFuture.runAsync(() -> {
            //1. 查出所有收货地址
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            confirmVo.setAddress(address);
        }, executor);

        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //2. 查出所有选中购物项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = cartFeignService.getCurrentUserCartItems();
            confirmVo.setItems(items);
        }, executor).thenRunAsync(()->{
            //4. 库存
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            R hasStock = wmsFeignService.getSkuHasStock(collect);
            List<SkuStockVo> data = hasStock.getData(new TypeReference<List<SkuStockVo>>() {
            });
            if (data != null) {
                Map<Long, Boolean> map = data.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        },executor);

        //3. 积分
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);
        // 等两个异步任务都完成
        CompletableFuture.allOf(getAddressFuture,cartFuture).get();

        // 4、防重令牌
        /**
         * 接口幂等性就是用户对同一操作发起的一次请求和多次请求结果是一致的
         * 不会因为多次点击而产生了副作用，比如支付场景，用户购买了商品，支付扣款成功，
         * 但是返回结果的时候出现了网络异常，此时钱已经扣了，用户再次点击按钮，
         * 此时就会进行第二次扣款，返回结果成功，用户查询余额发现多扣钱了，
         * 流水记录也变成了两条。。。这就没有保证接口幂等性
         */
        // 先是再页面中生成一个随机码把他叫做token先存到redis中，然后放到对象中在页面进行渲染。
        // 用户提交表单的时候，带着这个token和redis里面去匹配如果一直那么可以执行下面流程。
        // 匹配成功后再redis中删除这个token，下次请求再过来的时候就匹配不上直接返回
        // 生成防重令牌
        String token = UUID.randomUUID().toString().replace("-","");
        // 存到redis中 设置30分钟超时
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(),token,30, TimeUnit.MINUTES);
        // 放到页面进行显示token，然后订单中带着token来请求
        confirmVo.setOrderToken(token);
        return confirmVo;
    }

//    @GlobalTransactional
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        // 接收返回数据
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        // 通过拦截器拿到用户的数据
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        response.setCode(0);
        /**
         * 不使用原子性验证令牌
         *      1、用户带着两个订单，提交速度非常快，两个订单的令牌都是123，去redis里面查查到的也是123。
         *          两个对比都通过，然后来删除令牌，那么就会出现用户重复提交的问题，
         *      2、第一次差的快，第二次查的慢，只要没删就会出现这些问题
         *      3、因此令牌的【验证和删除必须保证原子性】
         *      String orderToken = vo.getOrderToken();
         *      String redisToken = redisTemplate.opsForValue().get(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
         *         if (orderToken != null && orderToken.equals(redisToken)) {
         *             // 令牌验证通过 进行删除
         *             redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
         *         } else {
         *             // 不通过
         *         }
         */
        // 验证令牌【令牌的对比和删除必须保证原子性】
        // 因此使用redis中脚本来进行验证并删除令牌
        // 0【删除失败/验证失败】 1【删除成功】
        String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
        /**
         * redis lur脚本命令解析
         * if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end
         *  1、redis调用get方法来获取一个key的值，如果这个get出来的值等于我们传过来的值
         *  2、然后就执行删除，根据这个key进行删除，删除成功返回1，验证失败返回0
         *  3、删除否则就是0
         *  总结：相同的进行删除，不相同的返回0
         * 脚本大致意思
         */
        // 拿到令牌
        String orderToken = vo.getOrderToken();
        /**
         * 	public <T> T execute(RedisScript<T> script // redis的脚本
         * 	    , List<K> keys // 对应的key 参数中使用了Array.asList 将参数转成list集合
         * 	    , Object... args) { // 要删除的值
         */
        // 原子验证和删除
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class)
                , Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId())
                , orderToken);
        if (result  == 0L) { // 验证令牌验证失败
            // 验证失败直接返回结果
            response.setCode(1);
            return response;
        } else { // 原子验证令牌成功
            // 下单 创建订单、验证令牌、验证价格、验证库存
            // 1、创建订单、订单项信息
            OrderCreateTo order = createOrder();
            // 2、应付总额 驗價
            BigDecimal payAmount = order.getOrder().getPayAmount();
            // 应付价格
            BigDecimal payPrice = vo.getPayPrice();
            /**
             * 电商项目对付款的金额精确到小数点后面两位
             * 订单创建好的应付总额 和购物车中计算好的应付价格求出绝对值。
             */
            if(Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 3.保存訂單
                // 金额对比成功 保存订单
                saveOrder(order);
                // 创建锁定库存Vo
                WareSkuLockVo lockVo = new WareSkuLockVo();
                // 订单号
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                // 准备好商品项
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    // skuid 用来查询商品信息
                    itemVo.setSkuId(item.getSkuId());
                    // 商品购买数量
                    itemVo.setCount(item.getSkuQuantity());
                    // 商品标题
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                // 商品项
                lockVo.setLocks(locks);
                // 4.遠程鎖庫存
                // 远程调用库存服务锁定库存
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) { // 库存锁定成功
                    // 将订单对象放到返回Vo中
                    response.setOrder(order.getOrder());
                    // 设置状态码
//                    response.setCode(0);
                    // 订单创建成功发送消息给MQ
//                    rabbitTemplate.convertAndSend("order-event-exchange"
//                            ,"order.create.order"
//                            ,order.getOrder());
                    // 5.遠程扣減積分
//                    int i = 10/0;
                    //TODO 訂單創建成功發送消息給MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order",order.getOrder());
                    return response;
                } else {
                    // 远程锁定库存失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
//                    response.setCode(3);
//                    return response;
                }
            }else {
              // 商品价格比较失败
              response.setCode(2);
              return response;
            }
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    /**
     * 关闭过期的的订单
     * @param entity
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        //因为消息发送过来的订单已经是很久前的了，中间可能被改动，因此要查询最新的订单
        OrderEntity orderEntity = this.getById(entity.getId());
        //如果订单还处于新创建的状态，说明超时未支付，进行关单
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity update = new OrderEntity();
            update.setId(entity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);

            //关单后发送消息通知其他服务进行关单相关的操作，如解锁库存
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            try {
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other",orderTo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity order = this.getOrderByOrderSn(orderSn);

        BigDecimal payAmount = order.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setOut_trade_no(orderSn);
        payVo.setTotal_amount(payAmount.toString());

        List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity entity = order_sn.get(0);
        payVo.setSubject(entity.getSkuName());
        payVo.setBody(entity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberRespVo.getId())
                .orderByDesc("id")
        );

        List<OrderEntity> order_sn = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(itemEntities);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(order_sn);

        return new PageUtils(page);
    }

    @Override
    public String handlePayResult(PayAsyncVo vo) {
        //保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        infoEntity.setSubject(vo.getSubject());
//        infoEntity.setCreateTime(new Date());
        paymentInfoService.save(infoEntity);

        //判断交易状态是否成功
        if (vo.getTrade_status().equals("TRADE_SUCCESS") || vo.getTrade_status().equals("TRADE_FINISHED")) {
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo, OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }

    @Override
    public void createSeckillOrder(SeckillOrderTo seckillOrder) {
        //1. 创建订单
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(seckillOrder.getOrderSn());
        orderEntity.setMemberId(seckillOrder.getMemberId());
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        BigDecimal multiply = seckillOrder.getSeckillPrice().multiply(new BigDecimal(""+seckillOrder.getNum()));
        orderEntity.setPayAmount(multiply);
//        orderEntity.setCreateTime(new Date());
         this.save(orderEntity);

        //2. 创建订单项
        OrderItemEntity orderItemEntity = new OrderItemEntity();
        orderItemEntity.setOrderSn(seckillOrder.getOrderSn());
        orderItemEntity.setRealAmount(multiply);
        orderItemEntity.setSkuQuantity(seckillOrder.getNum());
        //TODO 獲取當前SKU的詳細信息進行設置
//        R r = productFeignService.getSpuInfoBySkuId(seckillOrder.getSkuId());

        orderItemService.save(orderItemEntity);
    }

    /**
     * 保存訂單數據
     * @param order
     */
    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
//        orderEntity.setCreateTime(new Date());
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    /**
     * 创建订单和订单项
     * @return
     */
    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        // 1、生成订单号
        String orderSn = IdWorker.getTimeId();
        // 2、构建订单
        OrderEntity orderEntity = buildOrder(orderSn);
        // 3、构建订单项
        List<OrderItemEntity> itemEntities = builderOrderItems(orderSn);
        // 4、设置价格、积分相关信息
        computPrice(orderEntity,itemEntities);
        // 5、设置订单
        createTo.setOrder(orderEntity);
        // 6、设置订单项
        createTo.setOrderItems(itemEntities);

        return createTo;
    }

    /**
     * 构建订单
     * @param orderSn
     * @return
     */
    private OrderEntity buildOrder(String orderSn) {
        // 拿到共享数据
        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();
        // 用户登录登录数据
        MemberRespVo respVo = LoginUserInterceptor.loginUser.get();

        OrderEntity entity = new OrderEntity();
        // 设置订单号
        entity.setOrderSn(orderSn);
        // 用户id
        entity.setMemberId(respVo.getId());
        // 根据用户收货地址id查询出用户的收获地址信息
        R fare = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fare.getData(new TypeReference<FareVo>() {
        });
        //将查询到的会员收货地址信息设置到订单对象中
        // 运费金额
        entity.setFreightAmount(fareResp.getFare());
        // 城市
        entity.setReceiverCity(fareResp.getAddress().getCity());
        // 详细地区
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        // 收货人姓名
        entity.setReceiverName(fareResp.getAddress().getName());
        // 收货人手机号
        entity.setReceiverPhone(fareResp.getAddress().getPhone());

        entity.setReceiverPhone(fareResp.getAddress().getPostCode());

        // 区
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        // 省份直辖市
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        // 订单刚创建状态设置为 待付款，用户支付成功后将该该状态改成已付款
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 自动确认时间
        entity.setAutoConfirmDay(7);

        return entity;
    }

    /**
     * 构建订单项
     * @param orderSn
     * @return
     */
    private List<OrderItemEntity> builderOrderItems(String orderSn) {
        // 获取购物车中选中的商品
        List<OrderItemVo> currentUserCartItem = cartFeignService.getCurrentUserCartItems();
        if (currentUserCartItem != null && currentUserCartItem.size() > 0) {
            List<OrderItemEntity> itemEntities = currentUserCartItem.stream().map(cartItem -> {
                // 构建订单项
                OrderItemEntity itemEntity = builderOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntities;
        }
        return null;
    }

    /**
     * 构建订单项信息
     * @param cartItem
     * @return
     */
    private OrderItemEntity builderOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 1、根据skuid查询关联的spuinfo信息
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo data = r.getData(new TypeReference<SpuInfoVo>() {
        });
        // 2、设置商品项spu信息
        // 品牌信息
        itemEntity.setSpuBrand(data.getBrandId().toString());
        // 商品分类信息
        itemEntity.setCategoryId(data.getCatalogId());
        // spuid
        itemEntity.setSpuId(data.getId());
        // spu_name 商品名字
        itemEntity.setSpuName(data.getSpuName());

        // 3、设置商品sku信息
        // skuid
        itemEntity.setSkuId(skuId);
        // 商品标题
        itemEntity.setSkuName(cartItem.getTitle());
        // 商品图片
        itemEntity.setSkuPic(cartItem.getImage());
        // 商品sku价格
        itemEntity.setSkuPrice(cartItem.getPrice());
        // 商品属性以 ; 拆分
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(skuAttr);
        // 商品购买数量
        itemEntity.setSkuQuantity(cartItem.getCount());

        // 4、设置商品优惠信息【不做】
        // 5、设置商品积分信息
        // 赠送积分 移弃小数值
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        // 赠送成长值
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());

        // 6、订单项的价格信息
        // 这里需要计算商品的分解信息
        // 商品促销分解金额
        itemEntity.setPromotionAmount(new BigDecimal("0"));
        // 优惠券优惠分解金额
        itemEntity.setCouponAmount(new BigDecimal("0"));
        // 积分优惠分解金额
        itemEntity.setIntegrationAmount(new BigDecimal("0"));
        // 商品价格乘以商品购买数量=总金额(未包含优惠信息)
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        // 总价格减去优惠卷-积分优惠-商品促销金额 = 总金额
        origin.subtract(itemEntity.getPromotionAmount())
                .subtract(itemEntity.getCouponAmount())
                .subtract(itemEntity.getIntegrationAmount());
        // 该商品经过优惠后的分解金额
        itemEntity.setRealAmount(origin);
        return itemEntity;
    }

    /**
     * 计算订单涉及到的积分、优惠卷抵扣、促销优惠信息等信息
     * @param orderEntity
     * @param itemEntities
     * @return
     */
    private OrderEntity computPrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        // 1、定义好相关金额，然后遍历购物项进行计算
        // 总价格
        BigDecimal total = new BigDecimal("0.0");
        //相关优惠信息
        // 优惠卷抵扣金额
        BigDecimal coupon = new BigDecimal("0.0");
        // 积分优惠金额
        BigDecimal integration = new BigDecimal("0.0");
        // 促销优惠金额
        BigDecimal promotion = new BigDecimal("0.0");
        // 积分
        BigDecimal gift = new BigDecimal("0.0");
        // 成长值
        BigDecimal growth = new BigDecimal("0.0");

        // 遍历订单项将所有的优惠信息进行相加
        for (OrderItemEntity entity : itemEntities) {
            coupon = coupon.add(entity.getCouponAmount()); // 优惠卷抵扣
            integration = integration.add(entity.getIntegrationAmount()); // 积分优惠分解金额
            promotion = promotion.add(entity.getPromotionAmount()); // 商品促销分解金额
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString())); // 赠送积分
            growth = growth.add(new BigDecimal(entity.getGiftGrowth())); // 赠送成长值
            total = total.add(entity.getRealAmount()); //优惠后的总金额
        }

        // 2、设置订单金额
        // 订单总金额
        orderEntity.setTotalAmount(total);
        // 应付总额 = 订单总额 + 运费信息
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        // 促销优化金额（促销价、满减、阶梯价）
        orderEntity.setPromotionAmount(promotion);
        // 优惠券抵扣金额
        orderEntity.setCouponAmount(coupon);

        // 3、设置积分信息
        // 订单购买后可以获得的成长值
        orderEntity.setGrowth(growth.intValue());
        // 积分抵扣金额
        orderEntity.setIntegrationAmount(integration);
        // 可以获得的积分
        orderEntity.setIntegration(gift.intValue());
        // 删除状态【0->未删除；1->已删除】
        orderEntity.setDeleteStatus(0);
        return orderEntity;
    }
}

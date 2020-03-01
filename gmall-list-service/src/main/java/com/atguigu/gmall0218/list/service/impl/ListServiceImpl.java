package com.atguigu.gmall0218.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0218.bean.SkuLsInfo;
import com.atguigu.gmall0218.bean.SkuLsParams;
import com.atguigu.gmall0218.bean.SkuLsResult;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;


    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";


    /**
     * @Author ShawnYang
     * @Date 2020/2/13 0013 16:32
     * @Description 上架：保存SkuLsInfo 这个数据到es
     * 修改人：
     * 修改时间：
     * 修改备注：
     * 实现注意：
     */
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        // PUT /movie_index/movie/1
        /*
            1.  定义动作
            2.  执行动作
         */
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        /*
            1.  定义dsl 语句
            2.  定义动作
            3.  执行动作
            4.  获取结果集
         */
        //定义查询语句
        String query = makeQueryStringForSearch(skuLsParams);
        //进行查询，从es中获得结果
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult =null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //结果包装成结果结果集
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);

        return skuLsResult;
    }

    /**
     * @Author ShawnYang
     * @Date 2020/2/15 0015 18:07
     * @Description 记录每个商品被访问的次数
     * 修改人：
     * 修改时间：
     * 修改备注：
     * 实现注意：
     */
    @Override
    public void incrHotScore(String skuId) {

        Jedis jedis = redisUtil.getJedis();

        //定义一个key
        String hotKey="hotScore";

        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);
        //按照一定的规则，更新es中的skuInfo
        if(hotScore%10==0){

            updateHotScore(skuId,Math.round(hotScore));

        }
    }

    private void updateHotScore(String skuId, long hotScore) {

        /*
        1 编写dsl语句
        2 定义动作
        3 执行
         */
        String upd="{\n" +
                "  \"doc\":{\n" +
                "    \"hotScore\":"+hotScore+"\n" +
                "  }\n" +
                "}\n";

        Update update = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();

        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    // 设置返回结果

    /**
     *
     * {
     *   "took": 19,
     *   "timed_out": false,
     *   "_shards": {
     *     "total": 5,
     *     "successful": 5,
     *     "skipped": 0,
     *     "failed": 0
     *   },
     *   "hits": {
     *     "total": 2,
     *     "max_score": null,
     *     "hits": [
     *       {
     *         "_index": "gmall",
     *         "_type": "SkuInfo",
     *         "_id": "3",
     *         "_score": null,
     *         "_source": {
     *           "id": "3",
     *           "price": 999,
     *           "skuName": "小米 红米5 Plus 全面屏拍照手机 全网通版 3GB+32GB 金色 移动联通电信4G手机 双卡双待",
     *           "catalog3Id": "61",
     *           "skuDefaultImg": "http://file.service.com/group1/M00/00/00/wKhDyVrvp0uAXEdMAABvI_LYeVc795.jpg",
     *           "hotScore": 0,
     *           "skuAttrValueList": [
     *             {
     *               "id": "3",
     *               "attrId": "23",
     *               "valueId": "13",
     *               "skuId": "3"
     *             },
     *             {
     *               "id": "4",
     *               "attrId": "24",
     *               "valueId": "16",
     *               "skuId": "3"
     *             }
     *           ]
     *         },
     *         "highlight": {
     *           "skuName": [
     *             "<span style=color:red>小米</span> 红米5 Plus 全面屏拍照<span style=color:red>手机</span> 全网通版 3GB+32GB 金色 移动联通电信4G<span style=color:red>手机</span> 双卡双待"
     *           ]
     *         },
     *         "sort": [
     *           "3"
     *         ]
     *       },
     *       {
     *         "_index": "gmall",
     *         "_type": "SkuInfo",
     *         "_id": "2",
     *         "_score": null,
     *         "_source": {
     *           "id": "2",
     *           "price": 1111,
     *           "skuName": "小米 红米Note5 全网通版 4GB+64GB 黑色 移动联通电信4G手机 双卡双待",
     *           "catalog3Id": "61",
     *           "skuDefaultImg": "http://file.service.com/group1/M00/00/00/wKhDyVrrzYqAFidJAABvI_LYeVc573.jpg",
     *           "hotScore": 0,
     *           "skuAttrValueList": [
     *             {
     *               "id": "1",
     *               "attrId": "23",
     *               "valueId": "13",
     *               "skuId": "2"
     *             },
     *             {
     *               "id": "2",
     *               "attrId": "24",
     *               "valueId": "16",
     *               "skuId": "2"
     *             }
     *           ]
     *         },
     *         "highlight": {
     *           "skuName": [
     *             "<span style=color:red>小米</span> 红米Note5 全网通版 4GB+64GB 黑色 移动联通电信4G<span style=color:red>手机</span> 双卡双待"
     *           ]
     *         },
     *         "sort": [
     *           "2"
     *         ]
     *       }
     *     ]
     *   },
     *   "aggregations": {
     *     "groupby_attr": {
     *       "doc_count_error_upper_bound": 0,
     *       "sum_other_doc_count": 0,
     *       "buckets": [
     *         {
     *           "key": "13",
     *           "doc_count": 2
     *         },
     *         {
     *           "key": "16",
     *           "doc_count": 2
     *         }
     *       ]
     *     }
     *   }
     * }
     * @param searchResult 通过dsl 语句查询出来的结果
     * @param skuLsParams 入力参数
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        // 声明对象
        SkuLsResult skuLsResult = new SkuLsResult();
//        List<SkuLsInfo> skuLsInfoList;
        // 声明一个集合来存储SkuLsInfo 数据
        ArrayList<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        // 给集合赋值 SkuLsInfo.clas 指的是source中的对象
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        // 循环遍历
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            // 获取skuName 的高亮
            if (hit.highlight!=null && hit.highlight.size()>0){
                Map<String, List<String>> highlight = hit.highlight;
                List<String> list = highlight.get("skuName");
                // 取出高亮的skuName
                String skuNameHI = list.get(0);// 因为其中只有一个数据
                // 把SkuName设置成高亮的值
                skuLsInfo.setSkuName(skuNameHI);
            }
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);
//        设置总条数 long total;
        skuLsResult.setTotal(searchResult.getTotal());
//        设置总页数 long totalPages;
        // 如何计算总页数
        // 10 条数据 每页显示3条  几页？ 4
        // long totalPages = searchResult.getTotal()%skuLsParams.getPageSize()==0?searchResult.getTotal()/skuLsParams.getPageSize():(searchResult.getTotal()/skuLsParams.getPageSize())+1;
        long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
//        List<String> attrValueIdList;
        // 声明一个集合来存储平台属性值Id
        ArrayList<String> stringArrayList = new ArrayList<>();
        // 获取平台属性值Id
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        // 循环遍历
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            stringArrayList.add(valueId);
        }
        skuLsResult.setAttrValueIdList(stringArrayList);
        return skuLsResult;
    }

    /**
     * GET gmall/SkuInfo/_search
     * {
     *   "query": {
     *     "bool": {
     *       "filter": [{"term": {"catalog3Id": "61"}},
     *       {"term": {"skuAttrValueList.valueId": "13"}}
     *       ],
     *       "must": [
     *         {"match": {
     *           "skuName": "小米手机"
     *         }}
     *       ]
     *     }
     *   },
     *   "highlight": {
     *     "pre_tags": ["<span style=color:red>"],
     *     "post_tags": ["</span>"],
     *     "fields": {"skuName": {}}
     *   },
     *   "from": 0,
     *   "size": 20,
     *   "sort": [
     *     {
     *       "id": {
     *         "order": "desc"
     *       }
     *     }
     *   ],
     *   "aggs": {
     *     "groupby_attr": {
     *       "terms": {
     *         "field": "skuAttrValueList.valueId",
     *         "size": 10
     *       }
     *     }
     *   }
     * }
     * @param skuLsParams
     * @return
     */
    // 完全根据手写的dsl 语句！
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 0 定义一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 0.1 创建 bool查询器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 0.1.1 按照keyword 也就是skuName 进行匹配查询
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // 创建match
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            // 创建must
            boolQueryBuilder.must(matchQueryBuilder);
            // 0.2 定义一个高亮设置器
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();

            // 0.2.1设置高亮的规则
            highlighter.field("skuName");
            highlighter.preTags("<span style=color:red>");
            highlighter.postTags("</span>");

            // 0.2.2将设置好的高亮对象放入查询器中
            searchSourceBuilder.highlight(highlighter);
        }

        // 0.1.2 按照平台属性值Id过滤
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环
            for (String valueId : skuLsParams.getValueId()) {
                // 创建term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                // 创建filter 并添加term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        // 0.1.3 三级分类Id过滤
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // 创建term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id",skuLsParams.getCatalog3Id());
            // 创建filter 并添加term
            boolQueryBuilder.filter(termQueryBuilder);
        }
        // 0.1.4 查询器执行boolquery功能
        searchSourceBuilder.query(boolQueryBuilder);

        //设置分页
        // from 从第几条开始查询
        // 10 条 每页 3  第一页 0 3 ，第二页 3,3 第三页 6，3
        //设置分页规则
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        //0.3 设置起始条数
        searchSourceBuilder.from(from);
        // 0.4 size 设置每页显示的条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        // 0.5 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        // 聚合
        // 0.6 创建一个聚合对象 aggs:--terms
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr");
        // 0.6.1 设置聚合对象term字段 "field": "skuAttrValueList.valueId"
        groupby_attr.field("skuAttrValueList.valueId");
        // 0.6.2 执行term字段查询 aggs 放入查询器
        searchSourceBuilder.aggregation(groupby_attr);
        // 1 执行后查询器转换成String类型 获得查询query语句
        String query = searchSourceBuilder.toString();
        System.out.println("query:="+query);
        return query;
    }
}

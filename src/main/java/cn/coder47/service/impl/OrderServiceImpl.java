package cn.coder47.service.impl;

import cn.coder47.repository.OrderRepository;
import cn.coder47.service.OrderService;
import com.alibaba.fastjson.JSON;
import cn.coder47.dto.PageResponse;
import cn.coder47.model.Order;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchScrollHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: zhaibo
 * @Date: 2021-08-31 15:55
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Resource
    OrderRepository orderRepository;

    @Resource
    ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void saveAll(List<Order> orders) {
        orderRepository.saveAll(orders);
    }

    @Override
    public void deleteById(Integer id) {
        orderRepository.deleteById(id);
    }

    @Override
    public void updateById(Order order) {
        orderRepository.save(order);
    }

    @Override
    public PageResponse<Order> findList(Order order, Integer pageIndex, Integer pageSize) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria()
                .and(new Criteria("orderDesc").contains(order.getOrderDesc()))
                .and(new Criteria("orderNo").is(order.getOrderNo())))
                .setPageable(PageRequest.of(pageIndex, pageSize));

        SearchHits<Order> searchHits = elasticsearchRestTemplate.search(criteriaQuery, Order.class);
        List<Order> result = searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
        PageResponse<Order> pageResponse = new PageResponse<>();
        pageResponse.setTotal(searchHits.getTotalHits());
        pageResponse.setResult(result);
        return pageResponse;
    }

    @Override
    public PageResponse<Order> findAll(Integer pageIndex, Integer pageSize) {
        Page<Order> page = orderRepository.findAll(PageRequest.of(pageIndex, pageSize));

        PageResponse<Order> pageResponse = new PageResponse<>();
        pageResponse.setTotal(page.getTotalElements());
        pageResponse.setResult(page.getContent());
        return pageResponse;
    }

    @Override
    public PageResponse<Order> findHighlight(Order order, Integer pageIndex, Integer pageSize) {
        if (order == null) {
            PageResponse<Order> pageResponse = new PageResponse<>();
            pageResponse.setTotal(0L);
            pageResponse.setResult(new ArrayList<>());
            return pageResponse;
        }

        CriteriaQuery criteriaQuery = this.buildCriteriaQuery(order, pageIndex, pageSize);

        SearchHits<Order> searchHits = elasticsearchRestTemplate.search(criteriaQuery, Order.class);

        List<Order> result = searchHits.get().map(e -> {
            Order element = e.getContent();
            element.setHighlights(e.getHighlightFields());
            return element;
        }).collect(Collectors.toList());

        PageResponse<Order> pageResponse = new PageResponse<>();
        pageResponse.setTotal(searchHits.getTotalHits());
        pageResponse.setResult(result);
        return pageResponse;
    }

    @Override
    public PageResponse<Order> findScroll(Order order, Integer pageIndex, Integer pageSize, String indexName, String scrollId) {
        CriteriaQuery criteriaQuery = this.buildCriteriaQuery(order, pageIndex, pageSize);

        SearchScrollHits<Order> searchHits;
        if (!"".equals(scrollId)) {
            searchHits = elasticsearchRestTemplate.searchScrollContinue(scrollId, 1000, Order.class, IndexCoordinates.of(indexName));
        } else {
            searchHits = elasticsearchRestTemplate.searchScrollStart(1000, criteriaQuery, Order.class, IndexCoordinates.of(indexName));
        }

        List<Order> result = searchHits.get().map(e -> {
            Order element = e.getContent();
            element.setHighlights(e.getHighlightFields());
            return element;
        }).collect(Collectors.toList());

        PageResponse<Order> pageResponse = new PageResponse<>();
        pageResponse.setTotal(searchHits.getTotalHits());
        pageResponse.setResult(result);
        return pageResponse;
    }

    @Override
    public Order findById(Integer id) {
        return orderRepository.findById(id).orElse(null);
    }

    @Override
    public void bulkUpdate(List<Order> orders) {
        List<UpdateQuery> updateQueryList = orders.stream().map(e -> UpdateQuery.builder(String.valueOf(e.getId()))
                .withDocument(Document.parse(JSON.toJSONString(e)))
                .build()).collect(Collectors.toList());
        elasticsearchRestTemplate.bulkUpdate(updateQueryList, Order.class);
    }

    @Override
    public List<String> suggest(String keyword) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria()
                .and(new Criteria("orderDesc").contains(keyword)));
        SearchHits<Order> searchHits = elasticsearchRestTemplate.search(criteriaQuery, Order.class);
        return searchHits.get().map(e -> {
            Order element = e.getContent();
            return element.getOrderDesc();
        }).collect(Collectors.toList());
    }

    private CriteriaQuery buildCriteriaQuery(Order order, Integer pageIndex, Integer pageSize) {
        CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria()
                .and(new Criteria("orderNo").is(order.getOrderNo()))
                .and(new Criteria("orderDesc").contains(order.getOrderDesc())))
                .setPageable(PageRequest.of(pageIndex, pageSize));

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("orderNo").field("orderDesc");
        highlightBuilder.requireFieldMatch(false);
        highlightBuilder.preTags("<h3 style=\"color:blue\">");
        highlightBuilder.postTags("</h3>");

        HighlightQuery highlightQuery = new HighlightQuery(highlightBuilder);
        criteriaQuery.setHighlightQuery(highlightQuery);
        return criteriaQuery;
    }
}

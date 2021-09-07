package ink.zhaibo.repository;

import ink.zhaibo.model.Order;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/**
 * @Author: zhaibo
 * @Description:
 * @Date: 2021-08-20 15:23
 */
public interface OrderRepository extends ElasticsearchRepository<Order, Integer> {
}

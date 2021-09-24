package ink.zhaibo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * @Author: zhaibo
 * @Date: 2021-09-01 16:15
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResponse<T> implements Serializable {

    private long total;

    private List<T> result;

    private String scrollId;
}

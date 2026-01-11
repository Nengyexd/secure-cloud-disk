package com.clouddisk.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BreadcrumbVO {
    private List<BreadcrumbItem> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BreadcrumbItem {
        private Long id;
        private String name;
        private String path;
    }
}

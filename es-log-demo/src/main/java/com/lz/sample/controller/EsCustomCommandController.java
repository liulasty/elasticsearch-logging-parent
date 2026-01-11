package com.lz.sample.controller;

import com.lz.sample.service.EsCustomCommandService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * ES 自定义命令控制器
 * 提供 HTTP 接口来执行基础的 ES 操作
 * @author Administrator
 */
@RestController
@RequestMapping("/es/custom")
@Slf4j
public class EsCustomCommandController {

    @Autowired
    private EsCustomCommandService esCustomCommandService;

    /**
     * 1. 通用执行方法
     * POST /es/custom/execute
     * Body: {"method": "GET", "endpoint": "/_cat/indices", "jsonBody": null}
     */
    @PostMapping("/execute")
    public String executeRequest(@RequestBody Map<String, String> params) {
        String method = params.get("method");
        String endpoint = params.get("endpoint");
        String jsonBody = params.get("jsonBody");
        return esCustomCommandService.executeRequest(method, endpoint, jsonBody);
    }

    /**
     * 2. 查看索引
     * GET /es/custom/indices?options=v
     */
    @GetMapping("/indices")
    public String listIndices(@RequestParam(required = false) String options) {
        return esCustomCommandService.listIndices(options);
    }

    /**
     * 3. 创建索引
     * POST /es/custom/indices/{indexName}
     */
    @PostMapping("/indices/{indexName}")
    public String createIndex(@PathVariable String indexName) {
        return esCustomCommandService.createIndex(indexName);
    }

    /**
     * 4. 向索引写数据
     * POST /es/custom/indices/{indexName}/doc
     * Body: { ... any json data ... }
     */
    @PostMapping("/indices/{indexName}/doc")
    public String writeData(@PathVariable String indexName, @RequestBody Object data) {
        return esCustomCommandService.writeData(indexName, data);
    }

    /**
     * 5. 搜索数据
     * POST /es/custom/indices/{indexName}/search
     * Body: { "query": { ... } } (Optional)
     */
    @PostMapping("/indices/{indexName}/search")
    public String searchData(@PathVariable String indexName, @RequestBody(required = false) String queryJson) {
        return esCustomCommandService.searchData(indexName, queryJson);
    }

    /**
     * 6. 查看索引详细信息
     * GET /es/custom/indices/{indexName}
     */
    @GetMapping("/indices/{indexName}")
    public String getIndexDetails(@PathVariable String indexName) {
        return esCustomCommandService.getIndexDetails(indexName);
    }

    /**
     * 7. 删除索引
     * DELETE /es/custom/indices/{indexName}
     */
    @DeleteMapping("/indices/{indexName}")
    public String deleteIndex(@PathVariable String indexName) {
        return esCustomCommandService.deleteIndex(indexName);
    }
}

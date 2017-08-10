package com.ych.shcm.userinf.action;

import java.util.Collection;
import javax.annotation.Resource;

import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ych.core.model.BaseWithCommonOperationResult;
import com.ych.core.model.CommonOperationResult;
import com.ych.shcm.o2o.model.Constants;

/**
 * 清理缓存的Action
 */
@Controller("shcm.userinf.action.CacheAction")
public class CacheAction {

    @Resource(name = Constants.CACHE_MANAGER)
    private CacheManager cacheManager;

    /**
     * 清理所有缓存
     *
     * @return 操作结果
     */
    @ResponseBody
    @RequestMapping("/console/clearCache")
    public BaseWithCommonOperationResult clearCache() {
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String name : cacheNames) {
            cacheManager.getCache(name).clear();
        }
        return new BaseWithCommonOperationResult(CommonOperationResult.Succeeded);
    }

}

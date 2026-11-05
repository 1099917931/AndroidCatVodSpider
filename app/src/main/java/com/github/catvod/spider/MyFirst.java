package com.github.catvod.spider;

import android.content.Context;
import java.util.HashMap;
import java.util.List;

public class MyFirst extends Spider {

    private String ext;

    @Override
    public void init(Context context, String ext) {
        this.ext = ext;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        return "{\"class\":["
                + "{\"type_id\":\"movie\",\"type_name\":\"电影\"},"
                + "{\"type_id\":\"tv\",\"type_name\":\"电视剧\"}"
                + "],\"filters\":{},\"list\":[]}";
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter,
                                  HashMap<String, String> extend) throws Exception {
        return "";
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        return "";
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        return "";
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        return "";
    }
}

package com.github.catvod.spider;

import android.content.Context;

import com.github.catvod.crawler.Spider;
import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * 通用苹果CMS（MacCMS v10）资源站 Spider。
 * ext 填采集接口地址即可用，例如 https://cj.lziapi.com/api.php/provide/vod/
 * 也支持 ext 为 {"api":"接口地址"} 的 JSON 形式。
 */
public class MyFirst extends Spider {

    private String api = "";

    @Override
    public void init(Context context, String ext) {
        String e = ext == null ? "" : ext.trim();
        if (e.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(e);
                e = obj.optString("api", obj.optString("url", "")).trim();
            } catch (Exception ignored) {
            }
        }
        this.api = e;
    }

    private String url(String query) {
        if (api.isEmpty()) return "";
        return api + (api.contains("?") ? "&" : "?") + query;
    }

    private JSONObject fetch(String query) {
        try {
            String res = OkHttp.string(url(query));
            if (res == null || res.isEmpty()) return new JSONObject();
            return new JSONObject(res);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    private JSONArray parseList(JSONArray arr) throws Exception {
        JSONArray list = new JSONArray();
        if (arr == null) return list;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject v = arr.getJSONObject(i);
            JSONObject item = new JSONObject();
            item.put("vod_id", v.optString("vod_id"));
            item.put("vod_name", v.optString("vod_name"));
            item.put("vod_pic", v.optString("vod_pic"));
            item.put("vod_remarks", v.optString("vod_remarks"));
            list.put(item);
        }
        return list;
    }

    private JSONArray valueArray(String[] names, String[] values) throws Exception {
        JSONArray arr = new JSONArray();
        for (int i = 0; i < names.length; i++) {
            JSONObject o = new JSONObject();
            o.put("n", names[i]);
            o.put("v", values[i]);
            arr.put(o);
        }
        return arr;
    }

    private JSONObject buildFilters(JSONArray classes) throws Exception {
        JSONObject filters = new JSONObject();
        int now = Calendar.getInstance().get(Calendar.YEAR);
        String[] yearNames = new String[13];
        String[] yearValues = new String[13];
        yearNames[0] = "全部";
        yearValues[0] = "";
        for (int i = 0; i < 12; i++) {
            yearNames[i + 1] = String.valueOf(now - i);
            yearValues[i + 1] = String.valueOf(now - i);
        }
        JSONArray year = valueArray(yearNames, yearValues);
        JSONArray area = valueArray(
                new String[]{"全部", "大陆", "香港", "台湾", "日本", "韩国", "美国", "英国", "法国", "德国", "泰国", "印度", "新加坡", "加拿大", "澳大利亚", "其他"},
                new String[]{"", "大陆", "香港", "台湾", "日本", "韩国", "美国", "英国", "法国", "德国", "泰国", "印度", "新加坡", "加拿大", "澳大利亚", "其他"});
        JSONArray lang = valueArray(
                new String[]{"全部", "国语", "粤语", "英语", "韩语", "日语"},
                new String[]{"", "国语", "粤语", "英语", "韩语", "日语"});
        JSONArray by = valueArray(
                new String[]{"时间", "人气", "评分"},
                new String[]{"time", "hits", "score"});
        for (int i = 0; i < classes.length(); i++) {
            String tid = classes.getJSONObject(i).optString("type_id");
            JSONArray group = new JSONArray();
            group.put(new JSONObject().put("key", "year").put("name", "年份").put("value", year));
            group.put(new JSONObject().put("key", "area").put("name", "地区").put("value", area));
            group.put(new JSONObject().put("key", "lang").put("name", "语言").put("value", lang));
            group.put(new JSONObject().put("key", "by").put("name", "排序").put("value", by));
            filters.put(tid, group);
        }
        return filters;
    }

    @Override
    public String homeContent(boolean filter) throws Exception {
        JSONObject res = fetch("ac=list");
        JSONArray classes = new JSONArray();
        JSONArray classArr = res.optJSONArray("class");
        if (classArr != null) {
            for (int i = 0; i < classArr.length(); i++) {
                JSONObject c = classArr.getJSONObject(i);
                JSONObject item = new JSONObject();
                item.put("type_id", c.optString("type_id"));
                item.put("type_name", c.optString("type_name"));
                classes.put(item);
            }
        }
        JSONObject result = new JSONObject();
        result.put("class", classes);
        if (filter) result.put("filters", buildFilters(classes));
        result.put("list", parseList(res.optJSONArray("list")));
        return result.toString();
    }

    @Override
    public String categoryContent(String tid, String pg, boolean filter, HashMap<String, String> extend) throws Exception {
        StringBuilder q = new StringBuilder("ac=list&t=" + tid + "&pg=" + pg);
        if (extend != null) {
            for (String key : new String[]{"year", "area", "lang", "by"}) {
                String v = extend.get(key);
                if (v != null && !v.isEmpty()) q.append("&").append(key).append("=").append(URLEncoder.encode(v, "UTF-8"));
            }
        }
        JSONObject res = fetch(q.toString());
        JSONObject result = new JSONObject();
        result.put("page", res.optInt("page", Integer.parseInt(pg)));
        result.put("pagecount", res.optInt("pagecount", 1));
        result.put("limit", res.optInt("limit", 20));
        result.put("total", res.optInt("total", 0));
        result.put("list", parseList(res.optJSONArray("list")));
        return result.toString();
    }

    @Override
    public String detailContent(List<String> ids) throws Exception {
        JSONObject result = new JSONObject();
        JSONArray list = new JSONArray();
        if (ids == null || ids.isEmpty()) {
            result.put("list", list);
            return result.toString();
        }
        JSONObject res = fetch("ac=detail&ids=" + URLEncoder.encode(ids.get(0), "UTF-8"));
        JSONArray arr = res.optJSONArray("list");
        if (arr != null && arr.length() > 0) {
            JSONObject v = arr.getJSONObject(0);
            JSONObject vod = new JSONObject();
            vod.put("vod_id", v.optString("vod_id"));
            vod.put("vod_name", v.optString("vod_name"));
            vod.put("vod_pic", v.optString("vod_pic"));
            vod.put("type_name", v.optString("type_name"));
            vod.put("vod_year", v.optString("vod_year"));
            vod.put("vod_area", v.optString("vod_area"));
            vod.put("vod_remarks", v.optString("vod_remarks"));
            vod.put("vod_actor", v.optString("vod_actor"));
            vod.put("vod_director", v.optString("vod_director"));
            vod.put("vod_content", v.optString("vod_content").replaceAll("<[^>]+>", "").trim());
            vod.put("vod_play_from", v.optString("vod_play_from"));
            vod.put("vod_play_url", v.optString("vod_play_url"));
            list.put(vod);
        }
        result.put("list", list);
        return result.toString();
    }

    @Override
    public String searchContent(String key, boolean quick) throws Exception {
        JSONObject res = fetch("ac=list&wd=" + URLEncoder.encode(key, "UTF-8"));
        JSONObject result = new JSONObject();
        result.put("list", parseList(res.optJSONArray("list")));
        return result.toString();
    }

    @Override
    public String playerContent(String flag, String id, List<String> vipFlags) throws Exception {
        JSONObject result = new JSONObject();
        String lower = id.toLowerCase();
        boolean direct = lower.contains(".m3u8") || lower.contains(".mp4") || lower.contains(".mkv")
                || lower.contains(".flv") || lower.contains(".mp3") || lower.endsWith(".ts");
        result.put("parse", direct ? 0 : 1);
        result.put("url", id);
        return result.toString();
    }
}

package com.ponysdk.framework;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class PDOM {
    private static Document document;

    private PDOM(String dom) {
        document = Jsoup.parse(dom);
    }

    public static Element getElementByPID(String dom, String id) {
        return document.getElementsByAttributeValue("pid", id).first().child(0);
    }

}

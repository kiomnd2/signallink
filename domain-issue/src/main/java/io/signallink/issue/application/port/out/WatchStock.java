package io.signallink.issue.application.port.out;

/** 뉴스 조회 대상 종목 (코드·이름). 워치리스트의 단위. */
public record WatchStock(String code, String name) {}

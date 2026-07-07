package io.signallink.common;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public final class TimeUtils {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private TimeUtils() {}

    public static OffsetDateTime nowKst() {
        return OffsetDateTime.now(KST);
    }

    public static LocalDate todayKst() {
        return LocalDate.now(KST);
    }
}

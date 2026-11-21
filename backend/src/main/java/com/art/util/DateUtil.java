package com.art.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具类
 * 基于 Java 8+ 的 java.time 包，提供常用日期格式化、解析、范围计算等功能
 */
public final class DateUtil {

    // 常用日期格式
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN);
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(TIME_PATTERN);
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    // 私有构造器，禁止实例化
    private DateUtil() {
        throw new UnsupportedOperationException("工具类不允许实例化");
    }

    // ==================== LocalDate 格式化与解析 ====================

    /**
     * 格式化 LocalDate 为字符串（yyyy-MM-dd）
     */
    public static String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FORMATTER);
    }

    /**
     * 解析字符串为 LocalDate（yyyy-MM-dd）
     */
    public static LocalDate parseDate(String dateStr) {
        return dateStr == null || dateStr.isEmpty() ? null : LocalDate.parse(dateStr, DATE_FORMATTER);
    }

    // ==================== LocalDateTime 格式化与解析 ====================

    /**
     * 格式化 LocalDateTime 为字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMATTER);
    }

    /**
     * 解析字符串为 LocalDateTime（yyyy-MM-dd HH:mm:ss）
     */
    public static LocalDateTime parseDateTime(String dateTimeStr) {
        return dateTimeStr == null || dateTimeStr.isEmpty() ? null : LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMATTER);
    }

    // ==================== LocalTime 格式化与解析 ====================

    /**
     * 格式化 LocalTime 为字符串（HH:mm:ss）
     */
    public static String formatTime(LocalTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    /**
     * 解析字符串为 LocalTime（HH:mm:ss）
     */
    public static LocalTime parseTime(String timeStr) {
        return timeStr == null || timeStr.isEmpty() ? null : LocalTime.parse(timeStr, TIME_FORMATTER);
    }

    // ==================== 周范围计算 ====================

    /**
     * 获取指定日期所在周的周一
     */
    public static LocalDate getMondayOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * 获取指定日期所在周的周日
     */
    public static LocalDate getSundayOfWeek(LocalDate date) {
        return date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    }

    /**
     * 获取本周开始日期（周一）
     */
    public static LocalDate getCurrentWeekStart() {
        return getMondayOfWeek(LocalDate.now());
    }

    /**
     * 获取本周结束日期（周日）
     */
    public static LocalDate getCurrentWeekEnd() {
        return getSundayOfWeek(LocalDate.now());
    }

    // ==================== 月范围计算 ====================

    /**
     * 获取指定日期所在月的第一天
     */
    public static LocalDate getFirstDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.firstDayOfMonth());
    }

    /**
     * 获取指定日期所在月的最后一天
     */
    public static LocalDate getLastDayOfMonth(LocalDate date) {
        return date.with(TemporalAdjusters.lastDayOfMonth());
    }

    /**
     * 获取当前月的第一天
     */
    public static LocalDate getCurrentMonthStart() {
        return getFirstDayOfMonth(LocalDate.now());
    }

    /**
     * 获取当前月的最后一天
     */
    public static LocalDate getCurrentMonthEnd() {
        return getLastDayOfMonth(LocalDate.now());
    }

    // ==================== 差值计算 ====================

    /**
     * 计算两个日期之间的天数差（end - start）
     */
    public static long daysBetween(LocalDate start, LocalDate end) {
        return ChronoUnit.DAYS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的小时差（end - start）
     */
    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.HOURS.between(start, end);
    }

    /**
     * 计算两个日期时间之间的分钟差（end - start）
     */
    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return ChronoUnit.MINUTES.between(start, end);
    }

    // ==================== 日期判断 ====================

    /**
     * 判断日期是否在指定范围（包含边界）
     */
    public static boolean isDateInRange(LocalDate date, LocalDate start, LocalDate end) {
        if (date == null || start == null || end == null) return false;
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * 判断日期时间是否在指定范围（包含边界）
     */
    public static boolean isDateTimeInRange(LocalDateTime dateTime, LocalDateTime start, LocalDateTime end) {
        if (dateTime == null || start == null || end == null) return false;
        return !dateTime.isBefore(start) && !dateTime.isAfter(end);
    }

    /**
     * 判断两个日期是否是同一天
     */
    public static boolean isSameDay(LocalDate date1, LocalDate date2) {
        return date1 != null && date2 != null && date1.equals(date2);
    }

    // ==================== 获取当前时间 ====================

    /**
     * 获取当前日期字符串（yyyy-MM-dd）
     */
    public static String getCurrentDateStr() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * 获取当前日期时间字符串（yyyy-MM-dd HH:mm:ss）
     */
    public static String getCurrentDateTimeStr() {
        return LocalDateTime.now().format(DATE_TIME_FORMATTER);
    }
}
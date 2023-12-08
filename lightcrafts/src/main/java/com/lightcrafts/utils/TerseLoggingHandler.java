/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;

public class TerseLoggingHandler extends StreamHandler {

    public TerseLoggingHandler(OutputStream stream) {
        super(stream, new TerseLoggingFormatter());
    }

    public void publish(LogRecord record) {
        super.publish(record);
        flush();
    }

    public void close() {
        flush();
    }
}

class TerseLoggingFormatter extends Formatter {

    private final static String format = "{0,date} {0,time}";
    private final static String format = "yyyy:MM:dd HH:mm:ss";
    private final static DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);

    private static final String LineSeparator = System.getProperty("line.separator");

    @Override
    public synchronized String format(LogRecord record) {
        final StringBuilder sb = new StringBuilder();
        final var dateTime = ZonedDateTime.ofInstant(record.getInstant(), ZoneId.systemDefault());
        sb.append(dateTime.format(formatter));
        sb.append(" ");
        String name = record.getSourceClassName();
        if (name == null) {
            name = record.getLoggerName();
        }
        sb.append(name);
        final String method = record.getSourceMethodName();
        if (method != null) {
            sb.append(" ");
            sb.append(method);
        }
        sb.append(LineSeparator);
        final String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(message);
        final Throwable thrown = record.getThrown();
        if (thrown != null) {
            sb.append(thrown.getClass().getName());
            final String thrownMessage = thrown.getMessage();
            if (thrownMessage != null) {
                sb.append(": ");
                sb.append(thrownMessage);
            }
        }
        sb.append(LineSeparator);
        return sb.toString();
    }
}

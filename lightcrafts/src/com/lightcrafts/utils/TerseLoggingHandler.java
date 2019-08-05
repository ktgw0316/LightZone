/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.StreamHandler;
import java.io.OutputStream;

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

    private Date date = new Date();
    private final static String format = "{0,date} {0,time}";
    private MessageFormat formatter;

    private Object[] args = new Object[1];

    private static String LineSeparator = System.getProperty("line.separator");

    public synchronized String format(LogRecord record) {
        StringBuffer sb = new StringBuffer();
        // Minimize memory allocations here.
        date.setTime(record.getMillis());
        args[0] = date;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(format);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
        }
        else {
            sb.append(record.getLoggerName());
        }
        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }
        sb.append(LineSeparator);
        String message = formatMessage(record);
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(message);
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            sb.append(thrown.getClass().getName());
            if (thrown.getMessage() != null) {
                sb.append(": ");
                sb.append(thrown.getMessage());
            }
        }
        sb.append(LineSeparator);
        return sb.toString();
    }
}

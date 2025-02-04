package dev.galal.jasperreports.rest.service;

import lombok.NoArgsConstructor;
import net.sf.jasperreports.engine.JRParameter;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class JRParameterParser {
    private static final String STRING_CLASS = String.class.getName();
    private static final String LONG_CLASS = Long.class.getName();
    private static final String INTEGER_CLASS = Integer.class.getName();
    private static final String SHORT_CLASS = Short.class.getName();
    private static final String FLOAT_CLASS = Float.class.getName();
    private static final String BOOLEAN_CLASS = Boolean.class.getName();
    private static final String DOUBLE_CLASS = Double.class.getName();
    private static final String BIG_DECIMAL_CLASS = BigDecimal.class.getName();
    private static final String SQL_DATE_CLASS = Date.class.getName();
    private static final String SQL_TIME_CLASS = Time.class.getName();
    private static final String SQL_TIMESTAMP_CLASS = Timestamp.class.getName();
    private static final String DATE_CLASS = java.util.Date.class.getName();

    public static Map<String, Object> parseTypedParams(List<JRParameter> parameters, Map<String, String> params) {
        var paramsDef = parameters.stream()
                .collect(toMap(JRParameter::getName, identity()));
        return params.entrySet().stream()
                .map(param -> parse(paramsDef, param))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static Map.Entry<String,Object> parse(Map<String, JRParameter> paramsDef, Map.Entry<String, String> param) {
        if(param.getValue() == null) {
            return Map.entry(param.getKey(), null);
        }
        var paramType =
                ofNullable(paramsDef.get(param.getKey()))
                        .map(JRParameter::getValueClassName)
                        .orElseThrow(() -> new IllegalStateException("Parameter is not defined in the report: " + param.getKey()));
        Object parsed;
        if(paramType.equals(STRING_CLASS)) {
            parsed = param.getValue();
        } else if(paramType.equals(LONG_CLASS)) {
            parsed = Long.parseLong(param.getValue());
        } else if(paramType.equals(INTEGER_CLASS)) {
            parsed = Integer.parseInt(param.getValue());
        } else if(paramType.equals(SHORT_CLASS)) {
            parsed = Short.parseShort(param.getValue());
        } else if(paramType.equals(FLOAT_CLASS)) {
            parsed = Float.parseFloat(param.getValue());
        } else if(paramType.equals(BOOLEAN_CLASS)) {
            parsed = Boolean.parseBoolean(param.getValue());
        } else if(paramType.equals(DOUBLE_CLASS)) {
            parsed = Double.parseDouble(param.getValue());
        } else if(paramType.equals(BIG_DECIMAL_CLASS)) {
            parsed = new BigDecimal(param.getValue());
        } else if(paramType.equals(SQL_DATE_CLASS)) {
            parsed = Date.valueOf(param.getValue());
        } else if(paramType.equals(DATE_CLASS)) {
            parsed = parseDate(param.getValue());
        }  else if(paramType.equals(SQL_TIME_CLASS)) {
            parsed = Time.valueOf(param.getValue());
        }  else if(paramType.equals(SQL_TIMESTAMP_CLASS)) {
            parsed = Timestamp.valueOf(param.getValue());
        } else {
            throw new UnsupportedOperationException("Parameter of type: [" + paramType + "] is not supported, please consider using parameters of type [String] instead");
        }
        return Map.entry(param.getKey(), parsed);
    }

    private static java.util.Date parseDate(String str) {
        java.util.Date date;
        try {
            date = DateFormat.getDateInstance().parse(str);
        } catch (ParseException e) {
            try {
                var instant = LocalDate.parse(str, ISO_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant();
                date = java.util.Date.from(instant);
            } catch (Exception ex) {
                throw new UnsupportedOperationException("Failed to parse Date: [" + str + "], supported format is YYYY-MM-dd.", ex);
            }
        }
        return date;
    }
}

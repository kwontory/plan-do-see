package com.plandosee.diary.common.web;

import java.beans.PropertyEditorSupport;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.beans.PropertyAccessException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DefaultBindingErrorProcessor;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

import com.plandosee.diary.common.domain.DateBounds;
import com.plandosee.diary.common.domain.FieldCodes;
import com.plandosee.diary.common.domain.NumberText;
import com.plandosee.diary.common.domain.TextInput;
import com.plandosee.diary.common.domain.UuidText;

/**
 * ADR-30: the one place where submitted text becomes typed values, for every form, query and path variable.
 * <ul>
 *   <li>Strings: {@link TextInput#normalize} (CRLF and CR to LF, stripped, null when nothing visible is left), the
 *       same normalization every command applies, so @NotBlank reports a value of only NBSP or zero-width spaces as
 *       required and the form, the command and the DB count the same characters (ADR-22, QA3-D1, IV-11).</li>
 *   <li>Dates (LocalDate, {@code yyyy-MM-dd}) and date-times (LocalDateTime, {@code yyyy-MM-ddTHH:mm[:ss[.fraction]]},
 *       Seoul local): ISO local forms only, so a value with an offset or {@code Z} is a format error rather than an
 *       offset silently dropped (IV-06). They must lie within {@link DateBounds} (ADR-29): outside the range,
 *       including a year with five or more digits that the parser cannot read (a browser date box accepts up to six),
 *       is the field error {@link FieldCodes#DATE_OUT_OF_RANGE} with arguments {0} = min, {1} = max and the code as
 *       default message; the rejected text stays in the form.</li>
 *   <li>Integers: {@link NumberText#parseInt} (ASCII digits only, IV-07). UUIDs (also path variables):
 *       {@link UuidText#parse} (canonical form only, IV-14); a bad path id is a 404 like an unknown one.</li>
 *   <li>Other conversion failures become a field error with the message key {@link #TYPE_MISMATCH_CODE} and no
 *       default message, instead of the framework message that would expose Java type names. The hidden edit
 *       version ({@value #VERSION_FIELD}) has no visible box, so its failure is the global error
 *       {@link FieldCodes#VERSION_INVALID} (IV-13).</li>
 * </ul>
 */
@ControllerAdvice
public class FormBindingAdvice {

    public static final String TYPE_MISMATCH_CODE = "validation.typeMismatch";
    /** ADR-18 hidden field of every edit form. */
    public static final String VERSION_FIELD = "version";

    @InitBinder
    public void trimStrings(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new SubmittedTextEditor());
        binder.registerCustomEditor(LocalDate.class, new BoundedDateEditor());
        binder.registerCustomEditor(LocalDateTime.class, new BoundedDateTimeEditor());
        binder.registerCustomEditor(Integer.class, new StrictIntegerEditor());
        binder.registerCustomEditor(UUID.class, new StrictUuidEditor());
        binder.setBindingErrorProcessor(new FriendlyBindingErrorProcessor());
    }

    /** CRLF and lone CR to LF; null for null input (TextInput.normalizeLineBreaks). */
    public static String normalizeLineBreaks(String value) {
        return TextInput.normalizeLineBreaks(value);
    }

    static final class SubmittedTextEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            setValue(TextInput.normalize(text));
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value == null ? "" : value.toString();
        }
    }

    /**
     * A local date or date-time whose year has five or more digits: out of range rather than a format error. A value
     * with an offset is not local and stays a format error (IV-06).
     */
    private static final Pattern LONG_YEAR =
            Pattern.compile("^[+-]?\\d{5,}-\\d{1,2}-\\d{1,2}([T ]\\d{1,2}:\\d{2}(:\\d{2}(\\.\\d{1,9})?)?)?$");

    /** Thrown by the date editors for a value outside DateBounds; turned into the out-of-range field error. */
    static final class DateOutOfRangeException extends IllegalArgumentException {

        DateOutOfRangeException() {
            super(FieldCodes.DATE_OUT_OF_RANGE);
        }
    }

    static LocalDate parseDate(String text) {
        try {
            LocalDate date = DateTimeFormatter.ISO_LOCAL_DATE.parse(text, LocalDate::from);
            if (!DateBounds.contains(date)) {
                throw new DateOutOfRangeException();
            }
            return date;
        } catch (DateTimeException ex) {
            throw unreadable(text, ex);
        }
    }

    static LocalDateTime parseDateTime(String text) {
        try {
            LocalDateTime time = DateTimeFormatter.ISO_LOCAL_DATE_TIME.parse(text, LocalDateTime::from);
            if (!DateBounds.contains(time.toLocalDate())) {
                throw new DateOutOfRangeException();
            }
            return time;
        } catch (DateTimeException ex) {
            throw unreadable(text, ex);
        }
    }

    private static IllegalArgumentException unreadable(String text, DateTimeException ex) {
        if (LONG_YEAR.matcher(text).matches()) {
            return new DateOutOfRangeException();
        }
        return new IllegalArgumentException("unreadable date", ex);
    }

    /** yyyy-MM-dd within DateBounds; blank is null. Printed as ISO yyyy-MM-dd (the date input value format). */
    static final class BoundedDateEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            String value = text == null ? "" : text.strip();
            setValue(value.isEmpty() ? null : parseDate(value));
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value instanceof LocalDate date ? DateTimeFormatter.ISO_LOCAL_DATE.format(date) : "";
        }
    }

    /** datetime-local text within DateBounds (by its date); blank is null. Printed as ISO local date-time. */
    static final class BoundedDateTimeEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            String value = text == null ? "" : text.strip();
            setValue(value.isEmpty() ? null : parseDateTime(value));
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value instanceof LocalDateTime time ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time) : "";
        }
    }

    /** ASCII whole number within the int range (NumberText); blank is null. */
    static final class StrictIntegerEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            String value = text == null ? "" : text.strip();
            if (value.isEmpty()) {
                setValue(null);
                return;
            }
            Integer number = NumberText.parseInt(value);
            if (number == null) {
                throw new IllegalArgumentException("not a whole number");
            }
            setValue(number);
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value == null ? "" : value.toString();
        }
    }

    /** Canonical UUID text (UuidText); blank is null. */
    static final class StrictUuidEditor extends PropertyEditorSupport {

        @Override
        public void setAsText(String text) {
            String value = text == null ? "" : text.strip();
            if (value.isEmpty()) {
                setValue(null);
                return;
            }
            UUID id = UuidText.parse(value);
            if (id == null) {
                throw new IllegalArgumentException("not a canonical UUID");
            }
            setValue(id);
        }

        @Override
        public String getAsText() {
            Object value = getValue();
            return value == null ? "" : value.toString();
        }
    }

    static final class FriendlyBindingErrorProcessor extends DefaultBindingErrorProcessor {

        @Override
        public void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult) {
            String field = ex.getPropertyName();
            if (field == null) {
                super.processPropertyAccessException(ex, bindingResult);
                return;
            }
            if (VERSION_FIELD.equals(field)) {
                bindingResult.reject(FieldCodes.VERSION_INVALID, null, FieldCodes.VERSION_INVALID);
                return;
            }
            if (isOutOfRange(ex)) {
                bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                        new String[] {FieldCodes.DATE_OUT_OF_RANGE}, DateBounds.args(), FieldCodes.DATE_OUT_OF_RANGE));
                return;
            }
            // Framework codes first (a field-specific override may exist); the generic key is the last resort.
            String[] resolved = bindingResult.resolveMessageCodes(ex.getErrorCode(), field);
            String[] codes = Arrays.copyOf(resolved, resolved.length + 1);
            codes[resolved.length] = TYPE_MISMATCH_CODE;
            bindingResult.addError(new FieldError(bindingResult.getObjectName(), field, ex.getValue(), true,
                    codes, null, null));
        }

        private static boolean isOutOfRange(Throwable ex) {
            for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
                if (cause instanceof DateOutOfRangeException) {
                    return true;
                }
            }
            return false;
        }
    }
}

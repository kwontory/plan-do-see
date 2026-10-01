package com.plandosee.diary.export.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.ModelAndView;

import com.plandosee.diary.common.error.DomainRuleException;
import com.plandosee.diary.common.web.FormErrors;
import com.plandosee.diary.export.application.ExportService;
import com.plandosee.diary.export.domain.ExportRange;

import tools.jackson.databind.json.JsonMapper;

/**
 * JSON export of a chosen range.
 * <ul>
 *   <li>GET /export: view {@value #VIEW} with {@code exportForm} (from, to: the default range, about the last month up
 *       to today in Asia/Seoul) and {@code exportRangeMaxMonths} (1).</li>
 *   <li>GET /export/download?from=yyyy-MM-dd&amp;to=yyyy-MM-dd: a single UTF-8 JSON attachment named
 *       {@code plandosee-export-<from>-<to>.json}, {@code no-store}. GET never changes state. A missing, unreadable or
 *       rule-breaking range makes no file: the same view (200) with the field errors on {@code from} / {@code to} and
 *       the typed values kept. There is no default on this path.</li>
 * </ul>
 */
@Controller
public class ExportController {

    static final String VIEW = "export/index";

    private final ExportService exportService;
    private final JsonMapper jsonMapper;

    public ExportController(ExportService exportService, JsonMapper jsonMapper) {
        this.exportService = exportService;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping("/export")
    public String index(Model model) {
        model.addAttribute("exportForm", ExportForm.of(exportService.defaultRange()));
        return view(model);
    }

    /** The file (written here, request handled: null) or the page with the field errors. */
    @GetMapping("/export/download")
    public ModelAndView download(@Valid @ModelAttribute("exportForm") ExportForm form, BindingResult result, Model model,
                                 HttpServletResponse response) throws IOException {
        if (!result.hasErrors()) {
            try {
                ExportRange range = new ExportRange(form.getFrom(), form.getTo());
                Map<String, Object> document = exportService.export(range);
                response.setContentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8).toString());
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(ExportService.fileName(range)).build().toString());
                response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
                jsonMapper.writeValue(response.getOutputStream(), document);
                return null;
            } catch (DomainRuleException ex) {
                FormErrors.reject(result, ex);
            }
        }
        return new ModelAndView(view(model), model.asMap(), HttpStatus.OK);
    }

    private static String view(Model model) {
        model.addAttribute("exportRangeMaxMonths", ExportRange.MAX_MONTHS);
        return VIEW;
    }
}

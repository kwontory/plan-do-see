/*
 * PlanDoSee Note - progressive enhancement only.
 * Every feature works without this file. Button locking here is a UX aid; duplicate
 * completion is prevented on the server (idempotency key, row lock, unique constraint).
 * Never write user strings through innerHTML; this file only moves existing DOM nodes
 * and sets fixed textContent.
 * Elements are found by behaviour attributes only: data-js="flash",
 * data-js="error-summary", data-js="focus-first-invalid", data-js="busy-label", data-js="delete-confirm", data-no-lock,
 * data-submitting. data-js may hold several space-separated names (matched with ~=).
 * State written here for CSS: aria-busy on a busy button, data-focus-origin="script" on an element focused by this file.
 * Style classes (class) and test hooks (data-test) are never used as selectors here.
 */
(function () {
    'use strict';

    // Busy label comes from messages.properties (common.busy) through <meta name="pds-busy-text">.
    var busyMeta = document.querySelector('meta[name="pds-busy-text"]');
    var BUSY_TEXT = busyMeta ? busyMeta.getAttribute('content') : '...';
    var originals = new WeakMap();

    // The part of a button whose text changes while busy: a [data-js~="busy-label"] inside the button (so an icon or a
    // fixed-size round button keeps its shape), otherwise the whole button.
    function labelOf(button) {
        return button.querySelector('[data-js~="busy-label"]') || button;
    }

    function lock(button) {
        if (!button || originals.has(button)) {
            return;
        }
        var target = labelOf(button);
        var saved = [];
        target.childNodes.forEach(function (node) {
            saved.push(node.cloneNode(true));
        });
        originals.set(button, {target: target, nodes: saved});
        target.textContent = BUSY_TEXT;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
    }

    function unlock(button) {
        var saved = originals.get(button);
        if (!saved) {
            return;
        }
        saved.target.replaceChildren.apply(saved.target, saved.nodes.map(function (node) {
            return node.cloneNode(true);
        }));
        button.disabled = false;
        button.removeAttribute('aria-busy');
        originals.delete(button);
    }

    // Lock the button that submitted a POST form, so a double click sends one request.
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        if ((form.getAttribute('method') || 'get').toLowerCase() !== 'post') {
            return;
        }
        if (form.dataset.submitting === 'true') {
            event.preventDefault();
            return;
        }
        form.dataset.submitting = 'true';
        var button = event.submitter || form.querySelector('button[type="submit"], button:not([type])');
        if (button && button.hasAttribute('data-no-lock')) {
            return;
        }
        // Defer so the browser has already built the form data set before the button is disabled.
        window.setTimeout(function () {
            lock(button);
        }, 0);
    });

    // Restore buttons when the page comes back from the back/forward cache.
    window.addEventListener('pageshow', function () {
        document.querySelectorAll('form[data-submitting="true"]').forEach(function (form) {
            delete form.dataset.submitting;
            form.querySelectorAll('button').forEach(unlock);
        });
    });

    // Re-announce a flash message that was already present at load (after PRG redirect).
    // Screen readers often skip live-region content that exists before load, so the region is
    // emptied and the same nodes are put back shortly after. Nodes are moved, never re-parsed.
    function reannounceFlash() {
        var flash = document.querySelector('[data-js~="flash"]');
        if (!flash || !flash.textContent.trim()) {
            return;
        }
        reannounce(flash);
    }

    // Re-announce the "not saved" alert (role=alert) that was already in the page at load: like the flash, the
    // nodes are taken out and put back so screen readers read it, while focus stays on the first invalid field.
    function reannounce(region) {
        var nodes = Array.prototype.slice.call(region.childNodes);
        region.replaceChildren();
        window.setTimeout(function () {
            region.replaceChildren.apply(region, nodes);
        }, 150);
    }

    // Save failed: focus the first invalid field in screen order inside a form marked
    // data-js="focus-first-invalid". The field carries aria-invalid="true" and its aria-describedby starts with the
    // error text id, so the field, the error and the help are read together. With no focusable invalid field (only
    // global errors, a conflict, a read-only copy) the alert itself gets focus as before.
    document.addEventListener('DOMContentLoaded', function () {
        var summary = document.querySelector('[data-js~="error-summary"]');
        var invalid = document.querySelector('[data-js~="focus-first-invalid"] [aria-invalid="true"]');
        if (invalid) {
            invalid.focus();
            if (document.activeElement === invalid) {
                if (summary) {
                    reannounce(summary);
                }
                return;
            }
        }
        if (summary) {
            focusQuietly(summary);
            return;
        }
        reannounceFlash();
    });

    // Focus moved by this script (not by the user) is marked with data-focus-origin="script" until the element loses
    // focus, so the CSS can leave out the keyboard focus ring there (the box itself is already prominent). When the user
    // reaches the element with the keyboard later, the attribute is gone and the normal focus ring shows.
    function focusQuietly(element) {
        element.setAttribute('data-focus-origin', 'script');
        element.addEventListener('blur', function () {
            element.removeAttribute('data-focus-origin');
        }, {once: true});
        element.focus();
    }

    // Delete confirmation: Escape closes the open <details> and returns focus to its summary.
    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var details = event.target instanceof Element ? event.target.closest('[data-js~="delete-confirm"]') : null;
        if (!details || !details.open) {
            return;
        }
        details.open = false;
        var summary = details.querySelector('summary');
        if (summary) {
            summary.focus();
        }
    });
})();

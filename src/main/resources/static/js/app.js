/*
 * PlanDoSee Diary - progressive enhancement only.
 * Every feature works without this file. Button locking here is a UX aid; duplicate
 * completion is prevented on the server (idempotency key, row lock, unique constraint).
 * Never write user strings through innerHTML; this file only moves existing DOM nodes
 * and sets fixed textContent.
 */
(function () {
    'use strict';

    var BUSY_TEXT = '처리 중…';
    var originals = new WeakMap();

    function lock(button) {
        if (!button || originals.has(button)) {
            return;
        }
        var saved = [];
        button.childNodes.forEach(function (node) {
            saved.push(node.cloneNode(true));
        });
        originals.set(button, saved);
        button.textContent = BUSY_TEXT;
        button.disabled = true;
        button.setAttribute('aria-busy', 'true');
    }

    function unlock(button) {
        var saved = originals.get(button);
        if (!saved) {
            return;
        }
        button.replaceChildren.apply(button, saved.map(function (node) {
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

    // Move focus to the validation error summary after a failed submit.
    document.addEventListener('DOMContentLoaded', function () {
        var summary = document.getElementById('error-summary');
        if (summary) {
            summary.focus();
        }
    });

    // Delete confirmation: Escape closes the open <details> and returns focus to its summary.
    document.addEventListener('keydown', function (event) {
        if (event.key !== 'Escape') {
            return;
        }
        var details = event.target instanceof Element ? event.target.closest('details.delete[open]') : null;
        if (!details) {
            return;
        }
        details.open = false;
        var summary = details.querySelector('summary');
        if (summary) {
            summary.focus();
        }
    });
})();

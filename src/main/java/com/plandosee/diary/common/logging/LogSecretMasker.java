package com.plandosee.diary.common.logging;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Removes database connection details (JDBC URL, host, port, user name, password) from every rendered log line,
 * including exception messages and cause chains (T06-C58, ADR-25). Two layers:
 * <ol>
 * <li>exact values the application was configured with ({@link #registerDatasource}), and</li>
 * <li>generic shapes that drivers and pools write (JDBC URLs, {@code Connection to host:port refused},
 * {@code for user "x"}, unknown host names, IPv4 {@code address:port}, Supabase host names).</li>
 * </ol>
 * SQLState, error codes, exception types and stack frames stay, so a connection failure is still diagnosable.
 * The registry is static because logback creates the layout before any Spring bean exists.
 */
public final class LogSecretMasker {

    static final String URL_TOKEN = "<db-url>";
    static final String HOST_TOKEN = "<db-host>";
    static final String PORT_TOKEN = "<db-port>";
    static final String USER_TOKEN = "<db-user>";
    static final String PASSWORD_TOKEN = "<db-password>";

    private static final int MIN_USER_LENGTH = 3;
    private static final int MIN_BARE_PORT_DIGITS = 4;
    private static final int DEFAULT_POSTGRES_PORT = 5432;

    private static final String NAME_CHARS = "A-Za-z0-9_";

    private static final List<Rule> GENERIC_RULES = List.of(
            new Rule(Pattern.compile("jdbc:[A-Za-z0-9]+:[^\\s'\"()<>\\[\\]]+"), URL_TOKEN),
            new Rule(Pattern.compile("(?i)\\bpostgres(?:ql)?://[^\\s'\"()<>]+"), URL_TOKEN),
            new Rule(Pattern.compile("(Connection to )\\S+?( refused)"), "$1" + HOST_TOKEN + "$2"),
            new Rule(Pattern.compile("(UnknownHostException: )[^\\s:]+"), "$1" + HOST_TOKEN),
            new Rule(Pattern.compile("(?i)(for user )\"[^\"]*\""), "$1\"" + USER_TOKEN + "\""),
            new Rule(Pattern.compile("(?i)(for user )'[^']*'"), "$1'" + USER_TOKEN + "'"),
            new Rule(Pattern.compile("(?i)[A-Za-z0-9.-]*\\.supabase\\.(?:com|co|net)\\b"), HOST_TOKEN),
            new Rule(Pattern.compile("(?<![0-9.])\\d{1,3}(?:\\.\\d{1,3}){3}:\\d{1,5}(?![0-9])"),
                    HOST_TOKEN + ":" + PORT_TOKEN),
            new Rule(Pattern.compile(Pattern.quote(HOST_TOKEN) + ":\\d{1,5}(?![0-9])"), HOST_TOKEN + ":" + PORT_TOKEN));

    private static final Set<String> urls = new LinkedHashSet<>();
    private static final Set<String> hosts = new LinkedHashSet<>();
    private static final Set<String> ports = new LinkedHashSet<>();
    private static final Set<String> users = new LinkedHashSet<>();
    private static final Set<String> passwords = new LinkedHashSet<>();

    private static volatile List<Rule> valueRules = List.of();

    static {
        registerDatasource(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
    }

    private LogSecretMasker() {
    }

    /** Adds configured connection values to mask. Blank or null values are ignored; values accumulate. */
    public static synchronized void registerDatasource(String url, String username, String password) {
        boolean changed = false;
        if (hasText(url)) {
            changed |= urls.add(url.trim());
            for (HostPort hp : hostPorts(url.trim())) {
                changed |= hosts.add(hp.host());
                if (hp.port() != null) {
                    changed |= ports.add(hp.port());
                }
            }
        }
        if (hasText(username) && username.trim().length() >= MIN_USER_LENGTH) {
            changed |= users.add(username.trim());
        }
        if (hasText(password)) {
            changed |= passwords.add(password);
        }
        if (changed) {
            valueRules = buildValueRules();
        }
    }

    /** Returns the text with every known or recognisable database connection detail replaced by a token. */
    public static String mask(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        List<Rule> configured = valueRules;
        // Whole configured URLs first, so their parts are not half-replaced by the rules below.
        for (Rule rule : configured) {
            if (rule.urlRule()) {
                out = rule.apply(out);
            }
        }
        for (Rule rule : GENERIC_RULES) {
            out = rule.apply(out);
        }
        for (Rule rule : configured) {
            if (!rule.urlRule()) {
                out = rule.apply(out);
            }
        }
        return out;
    }

    private static List<Rule> buildValueRules() {
        List<Rule> rules = new ArrayList<>();
        for (String url : urls) {
            rules.add(new Rule(Pattern.compile(Pattern.quote(url)), URL_TOKEN, true));
        }
        for (String secret : passwords) {
            // Bounded like user names: a weak password such as "postgres" must not break "org.postgresql".
            rules.add(new Rule(bounded(secret), PASSWORD_TOKEN));
        }
        for (String host : hosts) {
            rules.add(new Rule(Pattern.compile("(?<![A-Za-z0-9.-])" + Pattern.quote(host)
                    + "(?![A-Za-z0-9-]|\\.[A-Za-z0-9])", Pattern.CASE_INSENSITIVE), HOST_TOKEN));
        }
        for (String user : users) {
            rules.add(new Rule(bounded(user), USER_TOKEN));
        }
        rules.add(new Rule(Pattern.compile(Pattern.quote(HOST_TOKEN) + ":\\d{1,5}(?![0-9])"),
                HOST_TOKEN + ":" + PORT_TOKEN));
        for (String port : ports) {
            rules.add(new Rule(Pattern.compile("(?<![0-9]):" + port + "(?![0-9])"), ":" + PORT_TOKEN));
            if (port.length() >= MIN_BARE_PORT_DIGITS) {
                rules.add(new Rule(Pattern.compile("(?<![0-9.:])" + port + "(?![0-9])"), PORT_TOKEN));
            }
        }
        return List.copyOf(rules);
    }

    private static Pattern bounded(String value) {
        return Pattern.compile("(?<![" + NAME_CHARS + "])" + Pattern.quote(value) + "(?![" + NAME_CHARS + "])");
    }

    /** host[:port] entries of a JDBC or libpq-style URL; supports several hosts and bracketed IPv6. */
    static List<HostPort> hostPorts(String url) {
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return List.of();
        }
        String rest = url.substring(scheme + 3);
        int end = rest.length();
        for (char stop : new char[] {'/', '?', '#'}) {
            int i = rest.indexOf(stop);
            if (i >= 0 && i < end) {
                end = i;
            }
        }
        String authority = rest.substring(0, end);
        int at = authority.lastIndexOf('@');
        if (at >= 0) {
            authority = authority.substring(at + 1);
        }
        List<HostPort> result = new ArrayList<>();
        for (String part : authority.split(",")) {
            String entry = part.trim();
            if (entry.isEmpty()) {
                continue;
            }
            String host;
            String port = null;
            if (entry.startsWith("[")) {
                int close = entry.indexOf(']');
                host = close > 0 ? entry.substring(0, close + 1) : entry;
                if (close > 0 && entry.length() > close + 2 && entry.charAt(close + 1) == ':') {
                    port = entry.substring(close + 2);
                }
            } else {
                int colon = entry.lastIndexOf(':');
                host = colon >= 0 ? entry.substring(0, colon) : entry;
                port = colon >= 0 ? entry.substring(colon + 1) : null;
            }
            if (port != null && !port.matches("\\d{1,5}")) {
                port = null;
            }
            if (port == null) {
                port = Integer.toString(DEFAULT_POSTGRES_PORT);
            }
            if (!host.isEmpty()) {
                result.add(new HostPort(host, port));
            }
        }
        return result;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    record HostPort(String host, String port) {
    }

    private record Rule(Pattern pattern, String replacement, boolean urlRule) {

        Rule(Pattern pattern, String replacement) {
            this(pattern, replacement, false);
        }

        String apply(String text) {
            Matcher m = pattern.matcher(text);
            return m.find() ? m.replaceAll(replacement.contains("$") ? replacement
                    : Matcher.quoteReplacement(replacement)) : text;
        }
    }
}

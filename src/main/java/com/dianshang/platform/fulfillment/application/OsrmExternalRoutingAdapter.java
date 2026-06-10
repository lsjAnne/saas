package com.dianshang.platform.fulfillment.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OsrmExternalRoutingAdapter implements ExternalRoutingAdapter {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private final ExternalRoutingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OsrmExternalRoutingAdapter(ExternalRoutingProperties properties,
                                      ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(Math.max(properties.getTimeoutMillis(), 500)))
                .build();
    }

    @Override
    public RoutePlanResult planRoute(RoutePlanRequest request) {
        String profile = resolveProfile(request.profile());
        if (!properties.isConfigured()) {
            return fallbackRoute(request, profile, "routing endpoint not configured");
        }
        try {
            JsonNode root = readJson(buildRouteUri(request, profile));
            JsonNode route = root.path("routes").path(0);
            if (!"Ok".equalsIgnoreCase(root.path("code").asText()) || route.isMissingNode()) {
                return fallbackRoute(request, profile, "osrm route result missing");
            }
            return new RoutePlanResult(
                    provider(),
                    profile,
                    "planned",
                    false,
                    "",
                    2,
                    round(route.path("distance").asDouble(0)),
                    round(route.path("duration").asDouble(0)),
                    true,
                    resolveHost(),
                    resolveMaskedEndpoint()
            );
        } catch (Exception exception) {
            return fallbackRoute(request, profile, "osrm route request failed");
        }
    }

    @Override
    public DistanceMatrixResult calculateDistanceMatrix(DistanceMatrixRequest request) {
        String profile = resolveProfile(request.profile());
        if (!properties.isConfigured()) {
            return fallbackMatrix(request, profile, "routing endpoint not configured");
        }
        try {
            JsonNode root = readJson(buildTableUri(request, profile));
            JsonNode distances = root.path("distances");
            JsonNode durations = root.path("durations");
            if (!"Ok".equalsIgnoreCase(root.path("code").asText())
                    || distances.isMissingNode()
                    || durations.isMissingNode()) {
                return fallbackMatrix(request, profile, "osrm matrix result missing");
            }
            return new DistanceMatrixResult(
                    provider(),
                    profile,
                    "calculated",
                    false,
                    "",
                    request.coordinates().size(),
                    parseMatrix(distances),
                    parseMatrix(durations),
                    true,
                    resolveHost(),
                    resolveMaskedEndpoint()
            );
        } catch (Exception exception) {
            return fallbackMatrix(request, profile, "osrm matrix request failed");
        }
    }

    @Override
    public RoutingAdapterSummary getSummary() {
        return new RoutingAdapterSummary(
                provider(),
                properties.isConfigured(),
                properties.isFallbackEnabled(),
                resolveProfile(null),
                resolveHost(),
                resolveMaskedEndpoint()
        );
    }

    private JsonNode readJson(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(Math.max(properties.getTimeoutMillis(), 500)))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return objectMapper.readTree(response.body());
    }

    private URI buildRouteUri(RoutePlanRequest request, String profile) {
        String coordinates = formatCoordinate(request.origin()) + ";" + formatCoordinate(request.destination());
        String query = "overview=false&alternatives=false&steps=false";
        return URI.create(trimTrailingSlash(properties.getEndpoint())
                + "/route/v1/"
                + encodePathSegment(profile)
                + "/"
                + coordinates
                + "?"
                + query);
    }

    private URI buildTableUri(DistanceMatrixRequest request, String profile) {
        String coordinates = request.coordinates().stream()
                .map(this::formatCoordinate)
                .reduce((left, right) -> left + ";" + right)
                .orElse("");
        return URI.create(trimTrailingSlash(properties.getEndpoint())
                + "/table/v1/"
                + encodePathSegment(profile)
                + "/"
                + coordinates
                + "?annotations=distance,duration");
    }

    private String formatCoordinate(RoutingCoordinate coordinate) {
        return coordinate.longitude() + "," + coordinate.latitude();
    }

    private String encodePathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private RoutePlanResult fallbackRoute(RoutePlanRequest request, String profile, String reason) {
        if (!properties.isFallbackEnabled()) {
            return new RoutePlanResult(
                    provider(),
                    profile,
                    "unavailable",
                    false,
                    reason,
                    2,
                    0,
                    0,
                    properties.isConfigured(),
                    resolveHost(),
                    resolveMaskedEndpoint()
            );
        }
        int distanceMeters = estimateDistanceMeters(request.origin(), request.destination());
        int durationSeconds = estimateDurationSeconds(distanceMeters, profile);
        return new RoutePlanResult(
                provider(),
                profile,
                "fallback_estimated",
                true,
                reason,
                2,
                distanceMeters,
                durationSeconds,
                properties.isConfigured(),
                resolveHost(),
                resolveMaskedEndpoint()
        );
    }

    private DistanceMatrixResult fallbackMatrix(DistanceMatrixRequest request, String profile, String reason) {
        List<List<Integer>> distances = new ArrayList<>();
        List<List<Integer>> durations = new ArrayList<>();
        for (RoutingCoordinate origin : request.coordinates()) {
            List<Integer> distanceRow = new ArrayList<>();
            List<Integer> durationRow = new ArrayList<>();
            for (RoutingCoordinate destination : request.coordinates()) {
                int distanceMeters = origin.equals(destination) ? 0 : estimateDistanceMeters(origin, destination);
                distanceRow.add(distanceMeters);
                durationRow.add(distanceMeters == 0 ? 0 : estimateDurationSeconds(distanceMeters, profile));
            }
            distances.add(List.copyOf(distanceRow));
            durations.add(List.copyOf(durationRow));
        }
        return new DistanceMatrixResult(
                provider(),
                profile,
                properties.isFallbackEnabled() ? "fallback_estimated" : "unavailable",
                properties.isFallbackEnabled(),
                reason,
                request.coordinates().size(),
                List.copyOf(distances),
                List.copyOf(durations),
                properties.isConfigured(),
                resolveHost(),
                resolveMaskedEndpoint()
        );
    }

    private int estimateDistanceMeters(RoutingCoordinate origin, RoutingCoordinate destination) {
        double latDistance = Math.toRadians(destination.latitude() - origin.latitude());
        double lonDistance = Math.toRadians(destination.longitude() - origin.longitude());
        double startLat = Math.toRadians(origin.latitude());
        double endLat = Math.toRadians(destination.latitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return round(EARTH_RADIUS_METERS * c);
    }

    private int estimateDurationSeconds(int distanceMeters, String profile) {
        double metersPerSecond = switch (profile.toLowerCase(Locale.ROOT)) {
            case "walking" -> 1.4d;
            case "cycling" -> 5.5d;
            default -> 15.0d;
        };
        return Math.max(1, round(distanceMeters / metersPerSecond));
    }

    private List<List<Integer>> parseMatrix(JsonNode node) {
        List<List<Integer>> matrix = new ArrayList<>();
        for (JsonNode rowNode : node) {
            List<Integer> row = new ArrayList<>();
            for (JsonNode valueNode : rowNode) {
                row.add(valueNode.isNumber() ? round(valueNode.asDouble()) : 0);
            }
            matrix.add(List.copyOf(row));
        }
        return List.copyOf(matrix);
    }

    private String resolveProfile(String requestProfile) {
        String value = requestProfile == null || requestProfile.isBlank()
                ? properties.getProfile()
                : requestProfile.trim();
        if (value == null || value.isBlank()) {
            return "driving";
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private String provider() {
        return properties.getProvider() == null || properties.getProvider().isBlank()
                ? "osrm"
                : properties.getProvider();
    }

    private String resolveHost() {
        if (!properties.isConfigured()) {
            return "";
        }
        try {
            URI uri = URI.create(properties.getEndpoint());
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String resolveMaskedEndpoint() {
        if (!properties.isConfigured()) {
            return "";
        }
        try {
            URI uri = URI.create(properties.getEndpoint());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme();
            String host = uri.getHost() == null ? "" : uri.getHost();
            String authority = host;
            if (uri.getPort() >= 0) {
                authority = authority + ":" + uri.getPort();
            }
            if (scheme.isBlank() || authority.isBlank()) {
                return "***";
            }
            return scheme + "://" + authority + "/***";
        } catch (IllegalArgumentException exception) {
            return "***";
        }
    }

    private String trimTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private int round(double value) {
        return (int) Math.round(value);
    }
}

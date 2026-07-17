package backend.fulfillment.application;

import java.util.List;

public interface ExternalRoutingAdapter {

    RoutePlanResult planRoute(RoutePlanRequest request);

    DistanceMatrixResult calculateDistanceMatrix(DistanceMatrixRequest request);

    RoutingAdapterSummary getSummary();

    record RoutingCoordinate(
            double longitude,
            double latitude
    ) {
    }

    record RoutePlanRequest(
            RoutingCoordinate origin,
            RoutingCoordinate destination,
            String profile
    ) {
    }

    record DistanceMatrixRequest(
            List<RoutingCoordinate> coordinates,
            String profile
    ) {
    }

    record RoutePlanResult(
            String provider,
            String profile,
            String routeStatus,
            boolean usedFallback,
            String fallbackReason,
            int waypointCount,
            int distanceMeters,
            int durationSeconds,
            boolean configured,
            String host,
            String maskedEndpoint
    ) {
    }

    record DistanceMatrixResult(
            String provider,
            String profile,
            String matrixStatus,
            boolean usedFallback,
            String fallbackReason,
            int coordinateCount,
            List<List<Integer>> distanceMatrixMeters,
            List<List<Integer>> durationMatrixSeconds,
            boolean configured,
            String host,
            String maskedEndpoint
    ) {
    }

    record RoutingAdapterSummary(
            String provider,
            boolean configured,
            boolean fallbackEnabled,
            String profile,
            String host,
            String maskedEndpoint
    ) {
    }
}


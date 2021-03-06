package org.netmelody.cieye.server.response.resource;

import java.io.IOException;

import org.netmelody.cieye.core.domain.Feature;
import org.netmelody.cieye.core.domain.Landscape;
import org.netmelody.cieye.core.logging.LogKeeper;
import org.netmelody.cieye.core.logging.Logbook;
import org.netmelody.cieye.server.CiSpyAllocator;
import org.netmelody.cieye.server.CiSpyHandler;
import org.netmelody.cieye.server.LandscapeFetcher;
import org.netmelody.cieye.server.response.RequestOriginTracker;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;
import org.simpleframework.http.resource.Resource;

public final class TargetNotationHandler implements Resource {

    private static final Logbook LOG = LogKeeper.logbookFor(TargetNotationHandler.class);
    
    private final RequestOriginTracker tracker;
    private final LandscapeFetcher landscapeFetcher;
    private final CiSpyAllocator spyAllocator;

    public TargetNotationHandler(LandscapeFetcher landscapeFetcher, CiSpyAllocator spyAllocator, RequestOriginTracker tracker) {
        this.landscapeFetcher = landscapeFetcher;
        this.spyAllocator = spyAllocator;
        this.tracker = tracker;
    }

    @Override
    public void handle(Request request, Response response) {
        try {
            final String targetId = request.getForm().get("id");
            final String note = request.getForm().get("note") + " by " + tracker.originOf(request);
            
            if (targetId == null || targetId.isEmpty()) {
                return;
            }
            
            final String[] segments = request.getAddress().getPath().getSegments();
            final Landscape landscape = landscapeFetcher.landscapeNamed(segments[segments.length - 2]);
            
            for (Feature feature : landscape.features()) {
                final CiSpyHandler spy = spyAllocator.spyFor(feature);
                if (spy.takeNoteOf(targetId, note)) {
                    return;
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to handle request to note a build", e);
        }
        finally {
            try {
                response.close();
            } catch (IOException e) {
                LOG.error("Failed to close response object", e);
            }
        }
    }
}

package protonema.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.jpos.transaction.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import protonema.Constants;
import protonema.service.ISO8583ServerService;

import java.util.HashMap;
import java.util.Map;

@Path("/api/iso")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProtonemaResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProtonemaResource.class);

    @Inject
    ISO8583ServerService isoServerService;

    @GET
    @Path("/status")
    public Response getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("application", "Protonema Quarkus ISO8583 Server");
        status.put("status", isoServerService.isRunning() ? "UP" : "DOWN");
        status.put("trxmanPort", 2333);
        status.put("srvPort", 9991);
        return Response.ok(status).build();
    }

    @POST
    @Path("/simulate")
    @SuppressWarnings("unchecked")
    public Response simulateTransaction(Map<String, String> requestData) {
        String mti = requestData.getOrDefault("mti", "0800");
        String stan = requestData.getOrDefault("stan", "000001");
        String pan = requestData.getOrDefault("pan", "6228480000000001");
        String cid = requestData.getOrDefault("cid", "1234567");

        try {
            GenericPackager packager = isoServerService.getISO87Packager();
            ISOMsg isoMsg = new ISOMsg();
            isoMsg.setPackager(packager);
            isoMsg.setMTI(mti);
            isoMsg.set(2, pan);
            isoMsg.set(11, stan);
            isoMsg.set(33, cid);
            isoMsg.set(70, "301");

            Context context = new Context();
            ISOMsg respMsg = (ISOMsg) isoMsg.clone();
            respMsg.setResponseMTI();
            respMsg.set(39, "00");

            context.put(Constants.REQUEST_KEY, isoMsg);
            context.put(Constants.RESPONSE_KEY, respMsg);

            Space<String, Context> space = SpaceFactory.getSpace("transient:default");
            space.out("TXNQueue", context, 10000L);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "QUEUED");
            result.put("mti", mti);
            result.put("stan", stan);
            result.put("rawPackedHex", org.jpos.iso.ISOUtil.hexString(isoMsg.pack()));

            return Response.ok(result).build();

        } catch (Exception e) {
            LOG.error("Failed to simulate ISO transaction", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(error).build();
        }
    }
}

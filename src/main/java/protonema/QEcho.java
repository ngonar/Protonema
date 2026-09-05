package protonema;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jdom2.Element;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.q2.QBeanSupport;
import org.jpos.space.Space;
import org.jpos.space.SpaceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

@ApplicationScoped
public class QEcho extends QBeanSupport implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(QEcho.class);

    @ConfigProperty(name = "protonema.echo.enabled", defaultValue = "true")
    boolean echoEnabled;

    private long tickInterval = 60000;

    public QEcho() {
        super();
    }

    @Override
    public void setPersist(Element e) {
        super.setPersist(e);
        if (e != null) {
            Iterator<?> it = e.getChildren("attr").iterator();
            while (it.hasNext()) {
                Element attr = (Element) it.next();
                if ("tickInterval".equals(attr.getAttributeValue("name"))) {
                    try {
                        this.tickInterval = Long.parseLong(attr.getTextTrim());
                    } catch (NumberFormatException ex) {
                        LOG.warn("Failed to parse tickInterval from XML element", ex);
                    }
                }
            }
        }
    }

    public void setTickInterval(long tickInterval) {
        this.tickInterval = tickInterval;
        setModified(true);
    }

    public long getTickInterval() {
        return tickInterval;
    }

    @Scheduled(every = "{protonema.echo.interval}")
    public void sendEcho() {
        if (!echoEnabled) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Space<String, Object> sp = SpaceFactory.getSpace("tspace:biller");

            ISOMsg m = new ISOMsg();
            m.setMTI("0800");
            m.set(11, "000001");
            String tgl = new SimpleDateFormat("yyyyMM").format(new Date());
            m.set(70, "301");
            m.set(41, "000001");

            GenericPackager packager = new GenericPackager(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("packager/iso87ascii.xml")
            );
            m.setPackager(packager);

            LOG.info("Echo request : {}", new String(m.pack()));

            sp.out("biller-send", m, 10000);
            ISOMsg response = (ISOMsg) sp.in("biller-receive", 10000);

            if (response != null) {
                if (response.getPackager() == null) {
                    response.setPackager(packager);
                }
                LOG.info("Echo response : {}", new String(response.pack()));
            } else {
                LOG.warn("Echo response timed out (10s)");
            }

        } catch (Exception e) {
            LOG.error("Error in QEcho execution", e);
        }
    }

    @Override
    public void run() {
        sendEcho();
    }
}

package protonema.service;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jpos.iso.ISOException;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.q2.iso.ChannelAdaptor;
import org.jpos.q2.iso.QMUX;
import org.jpos.q2.iso.QServer;
import org.jpos.transaction.TransactionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class ISO8583ServerService {

    private static final Logger LOG = LoggerFactory.getLogger(ISO8583ServerService.class);

    private TransactionManager transactionManager;
    private QServer serverSrv;
    private QServer serverTrxMan;
    private ChannelAdaptor billerChannelAdaptor;
    private QMUX qmux;

    private final AtomicBoolean running = new AtomicBoolean(false);

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initializing Protonema ISO 8583 Services from XML Deployment Descriptors...");

        try {
            SAXBuilder builder = new SAXBuilder();

            // 1. Load and initialize Transaction Manager (10_transaction_manager.xml)
            Element txnmgrEl = loadElement(builder, "deploy/10_transaction_manager.xml");
            if (txnmgrEl != null) {
                transactionManager = new TransactionManager();
                transactionManager.setPersist(txnmgrEl);
                transactionManager.init();
                transactionManager.start();
                LOG.info("Loaded and started TransactionManager from 10_transaction_manager.xml");
            }

            // 2. Load and initialize ServerTrxMan (15_server_port_trxman.xml)
            Element serverTrxManEl = loadElement(builder, "deploy/15_server_port_trxman.xml");
            if (serverTrxManEl != null) {
                serverTrxMan = new QServer();
                serverTrxMan.setPersist(serverTrxManEl);
                serverTrxMan.init();
                serverTrxMan.start();
                LOG.info("Loaded and started QServer (ServerTrxMan) from 15_server_port_trxman.xml");
            }

            // 3. Load and initialize Srv Server (11_serverport.xml)
            Element serverSrvEl = loadElement(builder, "deploy/11_serverport.xml");
            if (serverSrvEl != null) {
                serverSrv = new QServer();
                serverSrv.setPersist(serverSrvEl);
                serverSrv.init();
                serverSrv.start();
                LOG.info("Loaded and started QServer (srv) from 11_serverport.xml");
            }

            // 4. Load and initialize Biller Channel Adaptor (12_biller_channel.xml)
            Element billerChannelEl = loadElement(builder, "deploy/12_biller_channel.xml");
            if (billerChannelEl != null) {
                billerChannelAdaptor = new ChannelAdaptor();
                billerChannelAdaptor.setPersist(billerChannelEl);
                billerChannelAdaptor.init();
                billerChannelAdaptor.start();
                LOG.info("Loaded and started ChannelAdaptor from 12_biller_channel.xml");
            }

            // 5. Load and initialize QMUX (20_mux.xml)
            Element muxEl = loadElement(builder, "deploy/20_mux.xml");
            if (muxEl != null) {
                qmux = new QMUX();
                qmux.setPersist(muxEl);
                qmux.init();
                qmux.start();
                LOG.info("Loaded and started QMUX from 20_mux.xml");
            }

            running.set(true);
            LOG.info("All Protonema ISO 8583 XML Deployment Services started successfully.");

        } catch (Exception e) {
            LOG.error("Failed to start Protonema ISO 8583 XML Services", e);
        }
    }

    private Element loadElement(SAXBuilder builder, String resourcePath) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                LOG.warn("Resource not found: {}", resourcePath);
                return null;
            }
            return builder.build(is).getRootElement();
        } catch (Exception e) {
            LOG.error("Failed to load XML deployment file: {}", resourcePath, e);
            return null;
        }
    }

    void onStop(@Observes ShutdownEvent ev) {
        LOG.info("Stopping Protonema ISO 8583 Services...");
        running.set(false);

        if (serverSrv != null) {
            serverSrv.stop();
            serverSrv.destroy();
        }
        if (serverTrxMan != null) {
            serverTrxMan.stop();
            serverTrxMan.destroy();
        }
        if (billerChannelAdaptor != null) {
            billerChannelAdaptor.stop();
            billerChannelAdaptor.destroy();
        }
        if (qmux != null) {
            qmux.stop();
            qmux.destroy();
        }
        if (transactionManager != null) {
            transactionManager.stop();
            transactionManager.destroy();
        }
        LOG.info("Protonema ISO 8583 Services stopped.");
    }

    public boolean isRunning() {
        return running.get();
    }

    public GenericPackager getISO87Packager() throws ISOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("packager/iso87ascii.xml");
        return new GenericPackager(is);
    }
}

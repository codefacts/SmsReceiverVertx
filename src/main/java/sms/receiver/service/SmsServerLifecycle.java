package sms.receiver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smslib.AGateway;
import org.smslib.SMSLibException;
import org.smslib.Service;
import org.smslib.crypto.AESKey;
import org.smslib.modem.SerialModemGateway;
import sms.receiver.App;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.util.List;

/**
 * Created by shahadat on 3/8/16.
 */
public class SmsServerLifecycle {
    public final static Logger LOGGER = LoggerFactory.getLogger(SmsServerLifecycle.class);

    public void startSmsServer() {

        List<Integer> list = App.loadConfig().getJsonArray("com.ports").getList();

        list.forEach(port -> {

            try {
                tryPort(port);
                return;
            } catch (SMSLibException | IOException | InterruptedException e) {
                LOGGER.error("ERROR STARTING SMS SERVER ON PORT: " + port, e);
            }
        });

        LOGGER.info("FAILED SMS SERVER START ON ALL PORTS: " + list);
    }

    private void tryPort(int port) throws SMSLibException, InterruptedException, IOException {
        SerialModemGateway gateway = new SerialModemGateway("modem.com" + port, "COM" + port, 115200, "Huawei", "E160");
        gateway.setProtocol(AGateway.Protocols.PDU);
        gateway.setInbound(true);
        gateway.setOutbound(true);
        gateway.setSimPin("0000");

        Service.getInstance().addGateway(gateway);

        Service.getInstance().startService();
        System.out.println();
        System.out.println("Modem Information:");
        System.out.println("  Manufacturer: " + gateway.getManufacturer());
        System.out.println("  Model: " + gateway.getModel());
        System.out.println("  Serial No: " + gateway.getSerialNo());
        System.out.println("  SIM IMSI: " + gateway.getImsi());
        System.out.println("  Signal Level: " + gateway.getSignalLevel() + " dBm");
        System.out.println("  Battery Level: " + gateway.getBatteryLevel() + "%");
        System.out.println();
        Service.getInstance().getKeyManager().registerKey("+306948494037", new AESKey(new SecretKeySpec("0011223344556677".getBytes(), "AES")));

        LOGGER.info("SmsServerLauncher.start", "found port : " + port);
    }

    public void stop() {
        try {
            Service.getInstance().stopService();
        } catch (SMSLibException | InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

}

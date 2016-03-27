package sms.receiver.service;

import io.crm.promise.intfs.Promise;
import io.crm.web.util.WebUtils;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.UpdateResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shahadat on 3/8/16.
 */
public class DbInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(DbInitializer.class);

    public Promise<UpdateResult> createTables(JDBCClient jdbcClient) {
        return WebUtils.update(
            "CREATE TABLE `sms_inbox` (\n" +
                "  `id` int(11) NOT NULL,\n" +
                "  `endsWithMultiChar` int(19) NOT NULL DEFAULT '0',\n" +
                "  `memIndex` int(19) NOT NULL DEFAULT '0',\n" +
                "  `memLocation` varchar(19) NOT NULL DEFAULT '0',\n" +
                "  `mpMaxNo` int(19) NOT NULL DEFAULT '0',\n" +
                "  `mpMemIndex` int(19) NOT NULL DEFAULT '0',\n" +
                "  `mpRefNo` int(19) NOT NULL DEFAULT '0',\n" +
                "  `mpSeqNo` int(19) NOT NULL DEFAULT '0',\n" +
                "  `originator` varchar(20) NOT NULL DEFAULT '0',\n" +
                "  `pduUserData` varchar(756) NOT NULL DEFAULT '0',\n" +
                "  `pduUserDataHeader` varchar(756) NOT NULL DEFAULT '0',\n" +
                "  `smscNumber` varchar(500) NOT NULL DEFAULT '0',\n" +
                "  `date` date NOT NULL DEFAULT '0',\n" +
                "  `DCSMessageClass` varchar(500) NOT NULL DEFAULT '0',\n" +
                "  `dstPort` int(19) NOT NULL DEFAULT '0',\n" +
                "  `encoding` varchar(500) NOT NULL DEFAULT '0',\n" +
                "  `gatewayId` varchar(500) NOT NULL DEFAULT '0',\n" +
                "  `msgId` varchar(756) NOT NULL DEFAULT '0',\n" +
                "  `messageId` int(19) NOT NULL DEFAULT '0',\n" +
                "  `srcPort` int(19) NOT NULL DEFAULT '0',\n" +
                "  `text` text,\n" +
                "  `type` varchar(500) NOT NULL DEFAULT '0',\n" +
                "  `uuid` varchar(756) NOT NULL DEFAULT '0'\n" +
                ") ENGINE=InnoDB DEFAULT CHARSET=latin1".replace("\n", ""), jdbcClient)
        .mapToPromise(v -> WebUtils.update(
            "ALTER TABLE `sms_inbox`\n" +
                "  ADD PRIMARY KEY (`id`)".replace("\n", ""), jdbcClient))
        .mapToPromise(v -> WebUtils.update(
            "ALTER TABLE `sms_inbox`\n" +
                "  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT".replace("\n", ""), jdbcClient))
        ;
    }
}

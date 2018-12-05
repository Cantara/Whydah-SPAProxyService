package net.whydah.service.httpproxy.inn;

import net.whydah.sso.commands.baseclasses.BaseHttpGetHystrixCommand;

import java.net.URI;

public class CommandInnAPIGetSharedDeliveryAddress extends BaseHttpGetHystrixCommand<String> {
    private String myApplicationTokenId;
    private String nyUserTokenId;

    CommandInnAPIGetSharedDeliveryAddress(URI serviceUri, String myApplicationTokenId, String nyUserTokenId) {
        super(serviceUri, null, myApplicationTokenId, "InnGetaAPI", 10000);
        this.myApplicationTokenId = myApplicationTokenId;
        this.nyUserTokenId = nyUserTokenId;
    }

    @Override
    protected String getTargetPath() {
        return this.myApplicationTokenId + "/api/" + this.nyUserTokenId + "/shared-delivery-address";
    }
}

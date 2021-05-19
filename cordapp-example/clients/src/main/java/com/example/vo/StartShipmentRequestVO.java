package com.example.vo;
import com.example.model.AssetDetails;

import java.util.List;

public class StartShipmentRequestVO {

    private List<String> supplyChainParticipants;
    private AssetDetails assestDetails;

    public StartShipmentRequestVO() {
    }

    public StartShipmentRequestVO(List<String> supplyChainParticipants, AssetDetails assestDetails) {
        this.supplyChainParticipants = supplyChainParticipants;
        this.assestDetails = assestDetails;
    }

    public List<String> getSupplyChainParticipants() {
        return supplyChainParticipants;
    }

    public void setSupplyChainParticipants(List<String> supplyChainParticipants) {
        this.supplyChainParticipants = supplyChainParticipants;
    }

    public AssetDetails getAssestDetails() {
        return assestDetails;
    }

    public void setAssestDetails(AssetDetails assestDetails) {
        this.assestDetails = assestDetails;
    }

    @Override
    public String toString() {
        return "StartShipmentRequestVO{" +
                "supplyChainParticipants=" + supplyChainParticipants +
                ", assestDetails=" + assestDetails +
                '}';
    }
}

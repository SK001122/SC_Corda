package com.example.model;

import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class AssetDetails {

    private String assetID;
    private String originID;
    private String manuID;
    private String batchID;
    private String prodID;
    private String formulaID;
    private String ownerID;
    private String expiryID;
    private String stateID;
    //TODO: Set lat,long type to Double
    private String latitude;
    private String longitude;
    private String tempID;
    private String humidityID;
    private String vibrationID;

    @ConstructorForDeserialization
    public AssetDetails(String assetID, String originID, String manuID, String batchID, String prodID, String formulaID, String ownerID, String expiryID, String stateID, String latitude, String longitude, String tempID, String humidityID, String vibrationID) {
        this.assetID = assetID;
        this.originID = originID;
        this.manuID = manuID;
        this.batchID = batchID;
        this.prodID = prodID;
        this.formulaID = formulaID;
        this.ownerID = ownerID;
        this.expiryID = expiryID;
        this.stateID = stateID;
        this.latitude = latitude;
        this.longitude = longitude;
        this.tempID = tempID;
        this.humidityID = humidityID;
        this.vibrationID = vibrationID;
    }

    public AssetDetails() {
    }

    public String getAssetID() {
        return assetID;
    }

    public void setAssetID(String assetID) {
        this.assetID = assetID;
    }

    public String getOriginID() {
        return originID;
    }

    public void setOriginID(String originID) {
        this.originID = originID;
    }

    public String getManuID() {
        return manuID;
    }

    public void setManuID(String manuID) {
        this.manuID = manuID;
    }

    public String getBatchID() {
        return batchID;
    }

    public void setBatchID(String batchID) {
        this.batchID = batchID;
    }

    public String getProdID() {
        return prodID;
    }

    public void setProdID(String prodID) {
        this.prodID = prodID;
    }

    public String getFormulaID() {
        return formulaID;
    }

    public void setFormulaID(String formulaID) {
        this.formulaID = formulaID;
    }

    public String getOwnerID() {
        return ownerID;
    }

    public void setOwnerID(String ownerID) {
        this.ownerID = ownerID;
    }

    public String getExpiryID() {
        return expiryID;
    }

    public void setExpiryID(String expiryID) {
        this.expiryID = expiryID;
    }

    public String getStateID() {
        return stateID;
    }

    public void setStateID(String stateID) {
        this.stateID = stateID;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getTempID() {
        return tempID;
    }

    public void setTempID(String tempID) {
        this.tempID = tempID;
    }

    public String getHumidityID() {
        return humidityID;
    }

    public void setHumidityID(String humidityID) {
        this.humidityID = humidityID;
    }

    public String getVibrationID() {
        return vibrationID;
    }

    public void setVibrationID(String vibrationID) {
        this.vibrationID = vibrationID;
    }

    @Override
    public String toString() {
        return "AssetDetails{" +
                "assetID='" + assetID + '\'' +
                ", originID='" + originID + '\'' +
                ", manuID='" + manuID + '\'' +
                ", batchID='" + batchID + '\'' +
                ", prodID='" + prodID + '\'' +
                ", formulaID='" + formulaID + '\'' +
                ", ownerID='" + ownerID + '\'' +
                ", expiryID='" + expiryID + '\'' +
                ", stateID='" + stateID + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", tempID='" + tempID + '\'' +
                ", humidityID='" + humidityID + '\'' +
                ", vibrationID='" + vibrationID + '\'' +
                '}';
    }
}

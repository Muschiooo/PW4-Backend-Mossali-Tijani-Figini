package it.itsincom.webdev2023.service;

import io.vertx.ext.auth.impl.hash.SHA512;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HashCalculator {
    public String calculateHash(String password) {
        SHA512 algoritmo = new SHA512();
        return algoritmo.hash(null, password);
    }
}
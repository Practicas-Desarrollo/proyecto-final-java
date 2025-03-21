package com.concell.system.servicios;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

  @Value("${spring.application.security.jwt.secret-key}")
  private String secretKey;

  @Value("${spring.application.security.jwt.expiration}")
  private long tiempoExpiracion;

  public String crearToken(UserDetails userDetails) {
    Map<String, Object> extraClaims = new HashMap<>();
    return generarToken(extraClaims, userDetails);
  }

  public String getEmailFromToken(String token) {
    return obtenerClaim(token, Claims::getSubject);
  }

  public boolean esTokenValido(String token, UserDetails userDetails) {
    String email = getEmailFromToken(token);

    return (email.equals(userDetails.getUsername()) && !esTokenExpirado(token) );
  }

  private String generarToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    return Jwts
            .builder()
            .setClaims(extraClaims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + tiempoExpiracion))
            .signWith(obtenerKey(), SignatureAlgorithm.HS256)
            .compact();
  }

  private Key obtenerKey() {
    byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  private Claims obtenerClaims(String token) {
    return Jwts
            .parserBuilder()
            .setSigningKey(obtenerKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
  }

  public boolean esTokenExpirado(String token) {
    return obtenerExpiracion(token).before(new Date());
  }

  public Date obtenerExpiracion(String token) {
    return obtenerClaim(token, Claims::getExpiration);
  }

  public <T> T obtenerClaim(String token, Function<Claims, T> claimsResolver) {
    Claims claims = obtenerClaims(token);
    return claimsResolver.apply(claims);
  }
}

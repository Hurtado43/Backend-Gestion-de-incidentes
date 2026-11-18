package com.empresa.incidentes.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** Persona que usa la plataforma: reportante, tecnico o administrador. */
@Document(collection = "usuarios")
public class Usuario {

    @Id
    private String id;
    private String nombre;
    @Indexed(unique = true)
    private String email;
    private Rol rol;
    private String departamento;
    /** Solo para tecnicos: area de especialidad (por ejemplo, RED o SOFTWARE). */
    private Categoria especialidad;
    private boolean activo = true;
    private Instant fechaRegistro = Instant.now();

    public boolean esTecnico() {
        return rol == Rol.TECNICO;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
    public String getDepartamento() { return departamento; }
    public void setDepartamento(String departamento) { this.departamento = departamento; }
    public Categoria getEspecialidad() { return especialidad; }
    public void setEspecialidad(Categoria especialidad) { this.especialidad = especialidad; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Instant getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(Instant fechaRegistro) { this.fechaRegistro = fechaRegistro; }
}

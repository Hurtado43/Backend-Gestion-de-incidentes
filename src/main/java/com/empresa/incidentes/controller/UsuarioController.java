package com.empresa.incidentes.controller;

import com.empresa.incidentes.dto.Solicitudes.UsuarioRequest;
import com.empresa.incidentes.model.Usuario;
import com.empresa.incidentes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Reportantes, tecnicos y administradores")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar un usuario")
    public Usuario crear(@Valid @RequestBody UsuarioRequest req) {
        return usuarioService.crear(req);
    }

    @GetMapping
    @Operation(summary = "Listar todos los usuarios")
    public List<Usuario> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/tecnicos")
    @Operation(summary = "Listar los tecnicos activos")
    public List<Usuario> tecnicos() {
        return usuarioService.listarTecnicos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un usuario")
    public Usuario obtener(@PathVariable String id) {
        return usuarioService.obtener(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un usuario")
    public Usuario actualizar(@PathVariable String id, @Valid @RequestBody UsuarioRequest req) {
        return usuarioService.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Desactivar un usuario (baja logica)")
    public void desactivar(@PathVariable String id) {
        usuarioService.desactivar(id);
    }
}

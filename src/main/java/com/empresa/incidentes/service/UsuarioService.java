package com.empresa.incidentes.service;

import com.empresa.incidentes.dto.Solicitudes.UsuarioRequest;
import com.empresa.incidentes.exception.RecursoNoEncontradoException;
import com.empresa.incidentes.exception.ReglaNegocioException;
import com.empresa.incidentes.model.Rol;
import com.empresa.incidentes.model.Usuario;
import com.empresa.incidentes.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Gestion de reportantes, tecnicos y administradores. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario crear(UsuarioRequest req) {
        usuarioRepository.findByEmail(req.email()).ifPresent(u -> {
            throw new ReglaNegocioException("Ya existe un usuario con el email " + req.email());
        });
        Usuario usuario = new Usuario();
        copiarDatos(req, usuario);
        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> listarTecnicos() {
        return usuarioRepository.findByRolAndActivoTrue(Rol.TECNICO);
    }

    public Usuario obtener(String id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario", id));
    }

    public Usuario actualizar(String id, UsuarioRequest req) {
        Usuario usuario = obtener(id);
        usuarioRepository.findByEmail(req.email())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new ReglaNegocioException("Ya existe un usuario con el email " + req.email());
                });
        copiarDatos(req, usuario);
        return usuarioRepository.save(usuario);
    }

    /** Baja logica: el usuario se conserva para no romper el historial de incidentes. */
    public void desactivar(String id) {
        Usuario usuario = obtener(id);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private void copiarDatos(UsuarioRequest req, Usuario usuario) {
        usuario.setNombre(req.nombre());
        usuario.setEmail(req.email());
        usuario.setRol(req.rol());
        usuario.setDepartamento(req.departamento());
        usuario.setEspecialidad(req.rol() == Rol.TECNICO ? req.especialidad() : null);
    }
}

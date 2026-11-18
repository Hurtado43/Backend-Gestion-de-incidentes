package com.empresa.incidentes.repository;

import com.empresa.incidentes.model.Rol;
import com.empresa.incidentes.model.Usuario;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends MongoRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByRolAndActivoTrue(Rol rol);
}

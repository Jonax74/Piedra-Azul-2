export interface UsuarioAutenticado {
  user_id: string;
  username: string;
  roles: string[];
}

export interface PerfilUsuario {
  id: number;
  username: string;
  keycloak_user_id: string;
  estado: string;
  roles: string[];
}

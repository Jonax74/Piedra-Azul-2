export interface Persona {
  id?: number;
  primer_nombre: string;
  segundo_nombre?: string | null;
  primer_apellido: string;
  segundo_apellido?: string | null;
  genero: string;
  fecha_nacimiento: string;
  telefono: string;
  dni: number;
  correo?: string | null;
}

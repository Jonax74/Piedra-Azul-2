export interface Cita {
  id?: number;
  usuario?: number;
  paciente: number;
  medico: number;
  fecha_hora: string;
  estado: string;
  observacion?: string | null;
  paciente_detalle?: {
    persona?: {
      id?: number;
      primer_nombre?: string;
      primer_apellido?: string;
    };
  };
  medico_detalle?: {
    persona?: {
      id?: number;
      primer_nombre?: string;
      primer_apellido?: string;
    };
  };
}

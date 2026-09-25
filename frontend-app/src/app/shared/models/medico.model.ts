import { Especialidad } from './especialidad.model';

export interface Medico {
  id?: number;
  persona: number;
  tipo_profesional: string;
  estado: string;
  especialidades?: Especialidad[];
  especialidad_ids?: number[];
}

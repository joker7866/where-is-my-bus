import { createContext, useContext, useState } from 'react'
import type { ReactNode } from 'react'
import type { District } from './types'

/** Empty string means "all districts". */
interface DistrictContextValue {
  district: District
  setDistrict: (district: District) => void
}

const DistrictContext = createContext<DistrictContextValue | null>(null)

export function DistrictProvider({ children }: { children: ReactNode }) {
  const [district, setDistrict] = useState<District>('')
  return <DistrictContext.Provider value={{ district, setDistrict }}>{children}</DistrictContext.Provider>
}

export function useDistrict(): DistrictContextValue {
  const ctx = useContext(DistrictContext)
  if (!ctx) throw new Error('useDistrict must be used within a DistrictProvider')
  return ctx
}

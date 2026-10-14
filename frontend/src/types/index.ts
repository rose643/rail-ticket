export type Role = 'USER' | 'ADMIN'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface User {
  id: string
  username: string
  phone: string | null
  role: Role
  status: number
  createdAt: string
}

export interface LoginResponse {
  tokenType: string
  accessToken: string
  accessTokenExpiresAt: string
  refreshToken: string
  refreshTokenExpiresAt: string
  user: User
}

export interface CaptchaResponse {
  captchaId: string
  imageBase64: string
  expiresInSeconds: number
}

export interface Station {
  id: string
  code: string
  name: string
  city: string
  status: number
}

export interface StationSuggestion {
  type: 'CITY' | 'STATION'
  value: string
  label: string
  city: string
  stationCode: string | null
}

export interface SeatAvailability {
  seatTypeCode: string
  seatTypeName: string
  price: number
  available: number
}

export interface TrainSearchResult {
  scheduleId: string
  trainId: string
  trainNo: string
  trainName: string
  travelDate: string
  fromStationCode: string
  fromStationName: string
  fromStopIndex: number
  toStationCode: string
  toStationName: string
  toStopIndex: number
  departureTime: string
  arrivalTime: string
  durationMinutes: number | string
  seats: SeatAvailability[]
}

export interface Passenger {
  id: string
  name: string
  idType: string
  idNo: string
  phone: string
  createdAt: string
  updatedAt: string
}

export interface TicketOrder {
  id: string
  orderNo: string
  passengerUserId: string | null
  passengerName: string
  passengerIdType: string
  passengerIdNo: string
  passengerPhone: string
  scheduleId: string
  seatTypeCode: string
  fromStopIndex: number
  toStopIndex: number
  trainId: string
  trainNo: string
  trainName: string
  travelDate: string
  fromStationCode: string
  fromStationName: string
  toStationCode: string
  toStationName: string
  departureTime: string
  arrivalTime: string
  durationMinutes: number | string
  amount: number
  status: 'PENDING_PAYMENT' | 'PAID' | 'CLOSED' | 'REFUNDED'
  expireAt: string
  paidAt: string | null
  refundedAt: string | null
  createdAt: string
  updatedAt: string
}

export interface UserNotification {
  id: string
  notificationType: string
  title: string
  content: string
  businessType: string
  businessId: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export interface UserIdentity {
  id: string
  realName: string
  idType: string
  idNo: string
  status: number
  verifiedAt: string
}

export interface Train {
  id: string
  trainNo: string
  name: string
  status: number
}

export interface TrainStop {
  id: string
  trainId: string
  stationId: string
  stopIndex: number
  arrivalOffsetMinutes: number | null
  departureOffsetMinutes: number | null
}

export interface TrainSchedule {
  id: string
  trainId: string
  travelDate: string
  status: number
}

export interface TrainFare {
  id: string
  trainId: string
  seatTypeCode: string
  fromStopIndex: number
  toStopIndex: number
  price: number
}

export interface SeatInventory {
  id: string
  scheduleId: string
  seatTypeCode: string
  segmentIndex: number
  total: number
  available: number
}

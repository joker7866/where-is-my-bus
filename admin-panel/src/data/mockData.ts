import type { AppState } from './types'

// Mirrors the stop/route data in the companion Android app's MockBusRepository,
// so the two mock systems describe the same underlying (fictional) network.
export const initialState: AppState = {
  stops: [
    { id: 'GPM', name: 'Gandhipuram', tamilName: 'காந்திபுரம்', lat: 11.0168, lng: 76.9558, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'BUS_STAND' },
    { id: 'CCR', name: 'Cross Cut Road', tamilName: 'குறுக்குவெட்டி சாலை', lat: 11.0040, lng: 76.9603, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'TOWN' },
    { id: 'FM', name: 'Flower Market', tamilName: 'பூ சந்தை', lat: 11.0010, lng: 76.9650, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'LANDMARK' },
    { id: 'RS', name: 'Railway Station', tamilName: 'ரயில் நிலையம்', lat: 10.9985, lng: 76.9660, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'LANDMARK' },
    { id: 'TH', name: 'Town Hall', tamilName: 'டவுன் ஹால்', lat: 10.9980, lng: 76.9625, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'TOWN' },
    { id: 'UKK', name: 'Ukkadam', tamilName: 'உக்கடம்', lat: 10.9946, lng: 76.9558, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'BUS_STAND' },
    { id: 'SGN', name: 'Singanallur', tamilName: 'சிங்காநல்லூர்', lat: 11.0023, lng: 77.0304, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'TOWN' },
    { id: 'SVP', name: 'Saravanampatti', tamilName: 'சரவணம்பட்டி', lat: 11.0729, lng: 77.0075, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'TOWN' },
    { id: 'OND', name: 'Ondipudur', tamilName: 'ஒண்டிப்புதூர்', lat: 11.0100, lng: 77.0450, district: 'Coimbatore', city: 'Coimbatore', village: '', placeType: 'TOWN' },
    { id: 'TPT', name: 'Tirupattur', tamilName: 'திருப்பத்தூர்', lat: 10.1667, lng: 78.5667, district: 'Sivaganga', city: 'Tirupattur', village: '', placeType: 'TOWN' },
    { id: 'PLP', name: 'Pillayarpatti', tamilName: 'பிள்ளையார்பட்டி', lat: 10.1000, lng: 78.6300, district: 'Sivaganga', city: 'Karaikudi', village: 'Pillayarpatti', placeType: 'VILLAGE' },
    { id: 'KKD', name: 'Karaikudi', tamilName: 'காரைக்குடி', lat: 10.0767, lng: 78.7708, district: 'Sivaganga', city: 'Karaikudi', village: '', placeType: 'TOWN' },
  ],
  routes: [
    {
      id: 'R70A', shortName: '70A', longName: 'Gandhipuram → Ukkadam', busName: 'Gandhipuram Town Service',
      colorHex: '#145C45', startTime: '10:00',
      stops: [
        { stopId: 'GPM', travelMinFromPrev: 0, haltMin: 0 },
        { stopId: 'CCR', travelMinFromPrev: 6, haltMin: 1 },
        { stopId: 'FM', travelMinFromPrev: 6, haltMin: 1 },
        { stopId: 'RS', travelMinFromPrev: 8, haltMin: 1 },
        { stopId: 'TH', travelMinFromPrev: 7, haltMin: 1 },
        { stopId: 'UKK', travelMinFromPrev: 8, haltMin: 0 },
      ],
      liveMode: 'LIVE', district: 'Coimbatore', routeType: 'LOCAL', distanceKm: 7.5, viaTowns: '',
      operator: 'TNSTC', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 3, sharerCount: 9, lastSeenMinutesAgo: 0, timetableEtaMinutes: 0, delayMinutes: 5,
    },
    {
      id: 'R45', shortName: '45', longName: 'Gandhipuram → Saravanampatti', busName: 'Saravanampatti Shuttle',
      colorHex: '#1E8E5A', startTime: '06:30',
      stops: [
        { stopId: 'GPM', travelMinFromPrev: 0, haltMin: 0 },
        { stopId: 'SVP', travelMinFromPrev: 25, haltMin: 0 },
      ],
      liveMode: 'RECENT', district: 'Coimbatore', routeType: 'LOCAL', distanceKm: 9, viaTowns: '',
      operator: 'TNSTC', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 0, sharerCount: 5, lastSeenMinutesAgo: 3, timetableEtaMinutes: 0, delayMinutes: 0,
    },
    {
      id: 'R12', shortName: '12', longName: 'Ukkadam → Singanallur', busName: 'Singanallur Link',
      colorHex: '#6B7280', startTime: '07:00',
      stops: [
        { stopId: 'UKK', travelMinFromPrev: 0, haltMin: 0 },
        { stopId: 'SGN', travelMinFromPrev: 12, haltMin: 0 },
      ],
      liveMode: 'TIMETABLE', district: 'Coimbatore', routeType: 'LOCAL', distanceKm: 5, viaTowns: '',
      operator: 'TNSTC', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 0, sharerCount: 0, lastSeenMinutesAgo: 0, timetableEtaMinutes: 12, delayMinutes: 0,
    },
    {
      id: 'R96', shortName: '96', longName: 'Town Hall → Ondipudur', busName: 'Ondipudur Connector',
      colorHex: '#6B7280', startTime: '07:15',
      stops: [
        { stopId: 'TH', travelMinFromPrev: 0, haltMin: 0 },
        { stopId: 'OND', travelMinFromPrev: 18, haltMin: 0 },
      ],
      liveMode: 'TIMETABLE', district: 'Coimbatore', routeType: 'LOCAL', distanceKm: 6.5, viaTowns: '',
      operator: 'TNSTC', operatorType: 'GOVERNMENT', busType: 'TOWN', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 0, sharerCount: 0, lastSeenMinutesAgo: 0, timetableEtaMinutes: 18, delayMinutes: 0,
    },
    {
      id: 'R515', shortName: '515', longName: 'Tirupattur → Karaikudi', busName: 'Sivaganga Express',
      colorHex: '#145C45', startTime: '05:30',
      stops: [
        { stopId: 'TPT', travelMinFromPrev: 0, haltMin: 0 },
        { stopId: 'PLP', travelMinFromPrev: 20, haltMin: 2 },
        { stopId: 'KKD', travelMinFromPrev: 25, haltMin: 0 },
      ],
      liveMode: 'LIVE', district: 'Sivaganga', routeType: 'LONG_DISTANCE', distanceKm: 22, viaTowns: 'via Pillayarpatti',
      operator: 'TNSTC', operatorType: 'GOVERNMENT', busType: 'MOFUSSIL', acType: 'NON_AC', source: 'DEMO', verified: false,
      busesLive: 1, sharerCount: 2, lastSeenMinutesAgo: 0, timetableEtaMinutes: 0, delayMinutes: 0,
    },
  ],
  reports: [
    {
      id: 'REP1', type: 'STOP_FIX', message: '"Hope College" stop is 150 m further, near the signal',
      stopId: 'GPM', createdAt: '2026-10-06T09:05:00', status: 'OPEN',
    },
    {
      id: 'REP2', type: 'NO_SHOW', message: '9:30 AM bus did not come',
      routeId: 'R45', createdAt: '2026-10-06T09:31:00', status: 'OPEN',
    },
    {
      id: 'REP3', type: 'BREAKDOWN', message: 'Bus stopped near Railway Station, passengers moved',
      routeId: 'R70A', stopId: 'RS', createdAt: '2026-10-06T08:48:00', status: 'OPEN',
    },
    {
      id: 'REP4', type: 'STOP_FIX', message: 'Ukkadam stop sign is missing',
      stopId: 'UKK', createdAt: '2026-10-05T17:20:00', status: 'RESOLVED',
    },
  ],
  users: [
    {
      id: 'U1', name: 'Admin', email: 'admin@wheresmybus.app', role: 'SUPER_ADMIN',
      tripsShared: 0, joinedAt: '2026-01-10', active: true,
    },
    {
      id: 'U2', name: 'Karthik R', email: 'karthik.r@example.com', role: 'SHARER',
      tripsShared: 42, joinedAt: '2026-03-02', active: true,
    },
    {
      id: 'U3', name: 'Lakshmi N', email: 'lakshmi.n@example.com', role: 'SHARER',
      tripsShared: 18, joinedAt: '2026-05-14', active: true,
    },
    {
      id: 'U4', name: 'Suresh Kumar', email: 'suresh.k@example.com', role: 'ADMIN',
      tripsShared: 3, joinedAt: '2026-02-20', active: true,
    },
    {
      id: 'U5', name: 'Priya S', email: 'priya.s@example.com', role: 'SHARER',
      tripsShared: 7, joinedAt: '2026-08-01', active: false,
    },
  ],
  announcements: [
    {
      id: 'ANN1', title: 'Route 70A running 5 min late today',
      body: 'Heavy traffic near Railway Station is adding a few minutes to the 70A schedule this morning. We expect normal timings by afternoon.',
      severity: 'WARNING', district: 'Coimbatore', createdAt: '2026-10-06T07:30:00',
    },
    {
      id: 'ANN2', title: 'New stop added: Flower Market',
      body: 'Route 70A now stops at Flower Market, between Cross Cut Road and Railway Station, based on passenger requests.',
      severity: 'INFO', district: 'Coimbatore', createdAt: '2026-10-04T09:00:00',
    },
    {
      id: 'ANN3', title: 'Route 515 service begins',
      body: 'Live tracking is now available for the Tirupattur → Pillayarpatti → Karaikudi route. Share your location to help other riders.',
      severity: 'INFO', district: 'Sivaganga', createdAt: '2026-10-01T06:00:00',
    },
  ],
  districts: ['Coimbatore', 'Sivaganga'],
}

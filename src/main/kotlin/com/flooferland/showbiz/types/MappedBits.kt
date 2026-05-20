package com.flooferland.showbiz.types

import com.flooferland.showbiz.Showbiz
import com.flooferland.showbiz.show.BitId

/**
 * Bits that are mapped to a bit chart.
 *
 * Key = Chart ID
 *
 * Value = Bit ID
 */
class MappedBits() : HashMap<String, BitId>(Showbiz.charts.size)

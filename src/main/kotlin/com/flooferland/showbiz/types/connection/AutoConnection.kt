package com.flooferland.showbiz.types.connection

import com.flooferland.showbiz.blocks.entities.*
import com.flooferland.showbiz.entities.BotEntity
import com.flooferland.showbiz.utils.Extensions.applyChange

// TODO: Rework the automatic connection hell class
object AutoConnection {
    fun make(first: IConnectable, last: IConnectable) = when (last) {

        // Greybox

        is GreyboxBlockEntity if first is StagedBotBlockEntity -> {
            val (greybox, stagedBot) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(stagedBot)
            }
            "Staged Bot added"
        }
        is GreyboxBlockEntity if first is BotEntity -> {
            val (greybox, bot) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(bot)
            }
            "Bot added"
        }
        is GreyboxBlockEntity if first is ReelToReelBlockEntity -> {
            val (greybox, reelToReel) = Pair(last, first)
            reelToReel.applyChange(true) {
                show.bindListener(greybox)
                audio.bindListener(greybox)
                video.bindListener(greybox)
            }
            "Reel-to-reel added"
        }
        is GreyboxBlockEntity if first is ShowParserBlockEntity -> {
            val (greybox, showParser) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(showParser)
            }
            "Show parser added"
        }
        is GreyboxBlockEntity if first is SpotlightBlockEntity -> {
            val (greybox, spotlight) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(spotlight)
            }
            "Spotlight added"
        }
        is GreyboxBlockEntity if first is SpeakerBlockEntity -> {
            val (greybox, speaker) = Pair(last, first)
            greybox.applyChange(true) {
                audio.bindListener(speaker)
            }
            "Speaker added"
        }
        is GreyboxBlockEntity if first is CurtainControllerBlockEntity -> {
            val (greybox, curtainController) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(curtainController)
            }
            "Curtain Controller added"
        }
        is GreyboxBlockEntity if first is BitViewBlockEntity -> {
            val (greybox, bitView) = Pair(last, first)
            greybox.applyChange(true) {
                show.bindListener(bitView)
            }
            "Bit View added"
        }
        is GreyboxBlockEntity if first is MonitorBlockEntity -> {
            val (greybox, monitor) = Pair(last, first)
            greybox.applyChange(true) {
                video.bindListener(monitor)
            }
            "Monitor added"
        }


        // Other

        is CurtainControllerBlockEntity if first is CurtainBlockEntity -> {
            val (curtainController, curtain) = Pair(last, first)
            curtainController.applyChange(true) {
                control.bindListener(curtain)
            }
            "Bound the curtain"
        }
        is ShowSelectorBlockEntity if first is ShowParserBlockEntity -> {
            val (showSelector, showParser) = Pair(last, first)
            showSelector.applyChange(true) {
                show.bindListener(showParser)
            }
            "Show selector added"
        }
        is ShowSelectorBlockEntity if first is ReelToReelBlockEntity -> {
            val (showSelector, reelToReel) = Pair(last, first)
            showSelector.applyChange(true) {
                showSelect.bindListener(reelToReel)
            }
            "Show selector added"
        }
        is ReelToReelBlockEntity if first is ProgrammerBlockEntity -> {
            val (reelToReel, programmer) = Pair(last, first)
            reelToReel.applyChange(true) {
                show.bindListener(programmer)
            }
            programmer.applyChange(true) {
                show.bindListener(reelToReel)
            }
            "Programmer connected"
        }


        // Programmer

        is ProgrammerBlockEntity if first is ShowParserBlockEntity -> {
            val (programmer, showParser) = Pair(last, first)
            programmer.applyChange(true) {
                show.bindListener(showParser)
            }
            "Programmer connected"
        }
        is ProgrammerBlockEntity if first is StagedBotBlockEntity -> {
            val (programmer, bot) = Pair(last, first)
            programmer.applyChange(true) {
                show.bindListener(bot)
            }
            "Programmer connected"
        }
        else -> null
    }
}
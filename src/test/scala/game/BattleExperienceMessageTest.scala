// Copyright (c) 2017 PSForever
package game

import org.specs2.mutable._
import net.psforever.packet._
import net.psforever.packet.game._
import net.psforever.types.{ExperienceType, PlanetSideGUID}
import scodec.bits._

class BattleExperienceMessageTest extends Specification {
  val string = hex"B4 8A0A E7030000 00"

  "decode" in {
    PacketCoding.decodePacket(string).require match {
      case BattleExperienceMessage(player_guid, experience, unk) =>
        player_guid mustEqual PlanetSideGUID(2698)
        experience mustEqual 999
        unk mustEqual ExperienceType.Normal
      case _ =>
        ko
    }
  }

  "encode" in {
    val msg = BattleExperienceMessage(PlanetSideGUID(2698), 999, ExperienceType.Normal)
    val pkt = PacketCoding.encodePacket(msg).require.toByteVector

    pkt mustEqual string
  }
}

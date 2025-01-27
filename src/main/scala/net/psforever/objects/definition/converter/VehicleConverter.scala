// Copyright (c) 2017 PSForever
package net.psforever.objects.definition.converter

import net.psforever.objects.equipment.{Equipment, EquipmentSlot}
import net.psforever.objects.{PlanetSideGameObject, Vehicle}
import net.psforever.packet.game.objectcreate._
import net.psforever.types.{DriveState, PlanetSideGUID, VehicleFormat}

import scala.util.{Failure, Success, Try}

class VehicleConverter extends ObjectCreateConverter[Vehicle]() {
  override def DetailedConstructorData(obj: Vehicle): Try[VehicleData] =
    Failure(new Exception("VehicleConverter should not be used to generate detailed VehicleData (nothing should)"))

  override def ConstructorData(obj: Vehicle): Try[VehicleData] = {
    val health = StatConverter.Health(obj.Health, obj.MaxHealth)
    if (health > 0) { //active
      Success(
        VehicleData(
          PlacementData(obj.Position, obj.Orientation, obj.Velocity),
          CommonFieldData(
            obj.Faction,
            bops = false,
            alternate = false,
            v1 = false,
            v2 = None,
            jammered = obj.Jammed,
            v4 = Some(false),
            v5 = None,
            obj.Owner match {
              case Some(owner) => owner
              case None        => PlanetSideGUID(0)
            }
          ),
          unk3 = false,
          health,
          unk4 = false,
          no_mount_points = false,
          SterilizedDeploymentState(obj),
          unk5 = false,
          unk6 = false,
          obj.Cloaked,
          SpecificFormatData(obj),
          Some(InventoryData(MakeDriverSeat(obj) ++ MakeUtilities(obj) ++ MakeMountings(obj)))
        )(SpecificFormatModifier)
      )
    } else { //destroyed
      Success(
        VehicleData(
          PlacementData(obj.Position, obj.Orientation),
          CommonFieldData(
            obj.Faction,
            bops = false,
            alternate = true,
            v1 = false,
            v2 = None,
            jammered = obj.Jammed,
            v4 = Some(false),
            v5 = None,
            guid = PlanetSideGUID(0)
          ),
          unk3 = false,
          health = 0,
          unk4 = false,
          no_mount_points = true,
          driveState = DriveState.Mobile,
          unk5 = false,
          unk6 = false,
          cloak = false,
          SpecificFormatData(obj),
          inventory = None
        )(SpecificFormatModifier)
      )
    }
  }

  private def MakeDriverSeat(obj: Vehicle): List[InventoryItemData.InventoryItem] = {
    obj.Seats(0).occupant match {
      case Some(player) =>
        val offset: Long = MountableInventory.InitialStreamLengthToSeatEntries(obj.Velocity.nonEmpty, SpecificFormatModifier)
        List(InventoryItemData(ObjectClass.avatar, player.GUID, 0, SeatConverter.MakeSeat(player, offset)))
      case None =>
        Nil
    }
  }

  private def MakeMountings(obj: Vehicle): List[InventoryItemData.InventoryItem] = {
    obj.Weapons.collect {
      case (index, slot: EquipmentSlot) if slot.Equipment.nonEmpty =>
        val equip: Equipment = slot.Equipment.get
        val equipDef         = equip.Definition
        InventoryItemData(equipDef.ObjectId, equip.GUID, index, equipDef.Packet.ConstructorData(equip).get)
    }.toList
  }

  protected def MakeUtilities(obj: Vehicle): List[InventoryItemData.InventoryItem] = {
    Vehicle
      .EquipmentUtilities(obj.Utilities)
      .map({
        case (index, utilContainer) =>
          val util: PlanetSideGameObject = utilContainer()
          val utilDef                    = util.Definition
          InventoryItemData(utilDef.ObjectId, util.GUID, index, utilDef.Packet.ConstructorData(util).get)
      })
      .toList
  }

  private def SterilizedDeploymentState(obj: Vehicle): DriveState.Value = {
    obj.DeploymentState match {
      case state if state.id < 0 => DriveState.Mobile
      case state => state
    }
  }

  protected def SpecificFormatModifier: VehicleFormat = VehicleFormat.Normal

  protected def SpecificFormatData(obj: Vehicle): Option[SpecificVehicleData] = None
}

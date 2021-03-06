package org.ergoplatform.explorer.http.api.v1.models

import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Json}
import org.ergoplatform.explorer._
import org.ergoplatform.explorer.db.models.aggregates.{ExtendedUAsset, ExtendedUDataInput}
import org.ergoplatform.explorer.http.api.models.AssetInstanceInfo
import sttp.tapir.{Schema, SchemaType, Validator}

final case class UDataInputInfo(
  boxId: BoxId,
  value: Long,
  index: Int,
  outputBlockId: Option[BlockId],
  outputTransactionId: TxId,
  outputIndex: Int,
  ergoTree: ErgoTree,
  address: Address,
  assets: List[AssetInstanceInfo],
  additionalRegisters: Json
)

object UDataInputInfo {

  implicit val codec: Codec[UDataInputInfo] = deriveCodec

  implicit val schema: Schema[UDataInputInfo] =
    Schema
      .derived[UDataInputInfo]
      .modify(_.boxId)(_.description("ID of the corresponding box"))
      .modify(_.value)(_.description("Number of nanoErgs in the corresponding box"))
      .modify(_.index)(_.description("Index of the input in a transaction"))
      .modify(_.outputTransactionId)(
        _.description("ID of the transaction outputting corresponding box")
      )
      .modify(_.outputIndex)(_.description("Index of the output corresponding this input"))
      .modify(_.address)(_.description("Decoded address of the corresponding box holder"))

  implicit val validator: Validator[UDataInputInfo] = schema.validator

  implicit private def registersSchema: Schema[Json] =
    Schema(
      SchemaType.SOpenProduct(
        Schema(SchemaType.SString[Json]())
      )(_ => Map.empty)
    )

  def apply(i: ExtendedUDataInput, assets: List[ExtendedUAsset]): UDataInputInfo =
    UDataInputInfo(
      i.input.boxId,
      i.value,
      i.input.index,
      i.outputBlockId,
      i.outputTxId,
      i.outputIndex,
      i.ergoTree,
      i.address,
      assets.sortBy(_.index).map(AssetInstanceInfo(_)),
      i.additionalRegisters
    )

  def batch(ins: List[ExtendedUDataInput], assets: List[ExtendedUAsset]): List[UDataInputInfo] = {
    val groupedAssets = assets.groupBy(_.boxId)
    ins.sortBy(_.input.index).map(i => UDataInputInfo(i, groupedAssets.getOrElse(i.input.boxId, List.empty)))
  }
}

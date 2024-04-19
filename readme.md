PATH:
/POSTGetRemainsReserves: //uri
    post: // RequestMethod name
        tags:
            - Товары
        summary: Получение остатков и резервов WMS
        operationId: POSTGetRemainsReserves
        requestBody: //RequestBody
            content:
                application/json:
                    schema:
                        type: object
                        properties:
                            GUIDProduct:
                                type: array
                                items:
                                    type: string
                                    format: uuid
                                    description: Список уникальных идентификаторов товара
                            WareHouseCode:
                                maxLength: 9
                                type: string
                                description: Код склада WMS
                            ErpWareHouseCodes:
                                type: array
                                items:
                                    type: string
                                    description: Идентификаторы складов учетной системы
                                    ShowServiceCells:
                                        type: boolean
                                        description: |
                                          * true - получение остатков с учетом служебных ячеек (ячейки недостачи, брака, буферная зона, зона приема товара, зона контроля, зона коррекции остатков)
                                          * false - получение остатков без учета служебных ячеек
                                          required: false
      responses:
      200:
      description: Успех
      content:
      application/json:
      schema:
      type: array
      items:
      $ref: '#/components/schemas/RemainReserve'
      409:
      description: Ошибка обработки данных
      content:
      application/json:
      schema:
      $ref: '#/components/schemas/Error'
      x-codegen-request-body-name: body
